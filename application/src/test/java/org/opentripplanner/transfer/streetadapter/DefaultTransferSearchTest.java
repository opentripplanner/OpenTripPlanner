package org.opentripplanner.transfer.streetadapter;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.street.model.StreetModelForTest.intersectionVertex;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner._support.geometry.Coordinates;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.place.NearbyStopFinder;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.cost.CostLimit;
import org.opentripplanner.routing.linking.TransitStopVertexBuilderFactory;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.transfer.regular.api.maxduration.AllowedMode;
import org.opentripplanner.transit.transfer.regular.api.maxduration.BicycleMaxDurations;
import org.opentripplanner.transit.transfer.regular.api.maxduration.CarMaxDuration;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.spi.AllowedModesLookup;
import org.opentripplanner.transit.transfer.regular.spi.PathCriteria;
import org.opentripplanner.transit.transfer.regular.spi.TransferPath;

class DefaultTransferSearchTest {

  private static final FeedScopedId FROM = id(1);
  private static final FeedScopedId PLAIN = id(2);
  private static final FeedScopedId CAR = id(3);
  private static final FeedScopedId BICYCLE = id(4);
  private static final FeedScopedId BICYCLE_AND_CAR = id(5);

  private static final Map<FeedScopedId, Set<AllowedMode>> MODES = Map.of(
    CAR,
    Set.of(AllowedMode.CAR),
    BICYCLE,
    Set.of(AllowedMode.BICYCLE),
    BICYCLE_AND_CAR,
    Set.of(AllowedMode.BICYCLE, AllowedMode.CAR)
  );

  private final FakeNearbyStopFinder finder = new FakeNearbyStopFinder();

  @Test
  void aPlainMaxDurationSearchesAllStopsAndDropsThePathsThatAreTooLong() {
    finder.add(PLAIN, minutes(10)).add(CAR, minutes(40));
    var subject = subject(StreetMode.WALK, MODES, FROM);

    var result = subject.findNearbyStops(FROM, MaxDurationsPreference.of(Duration.ofMinutes(30)));

    assertThat(stopIds(result)).containsExactly(PLAIN);
    assertThat(finder.durationLimit).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void thereAreNoCarTransfersFromAStopThatDoesNotAllowCarsSoThereIsNoStreetSearch() {
    finder.add(CAR, minutes(10));
    var subject = subject(StreetMode.CAR, MODES, PLAIN);

    var result = subject.findNearbyStops(PLAIN, new CarMaxDuration(Duration.ofHours(3)));

    assertThat(result).isEmpty();
    assertThat(finder.calls).isEqualTo(0);
  }

  @Test
  void carTransfersFromAStopThatAllowsCarsGoToStopsThatAllowCarsOnly() {
    var from = id(6);
    var modes = Map.of(
      from,
      Set.of(AllowedMode.CAR),
      CAR,
      Set.of(AllowedMode.CAR),
      BICYCLE,
      Set.of(AllowedMode.BICYCLE)
    );
    finder.add(CAR, minutes(100)).add(BICYCLE, minutes(10)).add(PLAIN, minutes(10));
    var subject = subject(StreetMode.CAR, modes, from);

    var result = subject.findNearbyStops(from, new CarMaxDuration(Duration.ofHours(3)));

    assertThat(stopIds(result)).containsExactly(CAR);
    assertThat(finder.calls).isEqualTo(1);
    assertThat(finder.durationLimit).isEqualTo(Duration.ofHours(3));
  }

  @Test
  void aBicycleSearchFromAStopThatAllowsBicyclesUsesTheLongerDuration() {
    var preference = new BicycleMaxDurations(
      Duration.ofMinutes(20),
      Duration.ofMinutes(40),
      Duration.ofMinutes(60)
    );
    finder.add(PLAIN, minutes(10)).add(BICYCLE, minutes(30)).add(CAR, minutes(30));
    var modes = Map.of(FROM, Set.of(AllowedMode.BICYCLE), BICYCLE, Set.of(AllowedMode.BICYCLE));
    var subject = subject(StreetMode.BIKE, modes, FROM);

    var result = subject.findNearbyStops(FROM, preference);

    // The transfer to the stop that does not allow bicycles is too long to be accepted
    assertThat(stopIds(result)).containsExactly(PLAIN, BICYCLE);
    assertThat(finder.durationLimit).isEqualTo(Duration.ofMinutes(40));
  }

  @Test
  void aBicycleSearchFromAStopThatAllowsNothingUsesTheMaxDuration() {
    var preference = new BicycleMaxDurations(
      Duration.ofMinutes(20),
      Duration.ofMinutes(40),
      Duration.ofMinutes(60)
    );
    finder.add(PLAIN, minutes(10)).add(BICYCLE, minutes(30));
    var subject = subject(StreetMode.BIKE, MODES, FROM);

    var result = subject.findNearbyStops(FROM, preference);

    assertThat(stopIds(result)).containsExactly(PLAIN);
    assertThat(finder.durationLimit).isEqualTo(Duration.ofMinutes(20));
  }

  @Test
  void thereIsNoStreetSearchFromAStopThatIsNotATransferSource() {
    finder.add(PLAIN, minutes(10));
    var subject = subject(StreetMode.WALK, MODES, FROM, stopId -> false, stopId -> true);

    var result = subject.findNearbyStops(FROM, MaxDurationsPreference.of(Duration.ofMinutes(30)));

    assertThat(result).isEmpty();
    assertThat(finder.calls).isEqualTo(0);
  }

  @Test
  void thePathsToAStopThatIsNotATransferTargetAreDropped() {
    finder.add(PLAIN, minutes(10)).add(CAR, minutes(10));
    var subject = subject(
      StreetMode.WALK,
      MODES,
      FROM,
      stopId -> true,
      stopId -> !stopId.equals(PLAIN)
    );

    var result = subject.findNearbyStops(FROM, MaxDurationsPreference.of(Duration.ofMinutes(30)));

    assertThat(stopIds(result)).containsExactly(CAR);
  }

  /** The cost of a path is capped at the max cost Raptor accepts. */
  @Nested
  class CostLimitOfAPath {

    private static final int MAX_RAPTOR_C1 = RaptorCostConverter.toRaptorCost(
      (double) CostLimit.MAX_COST
    );

    private final DefaultTransferSearch subject = subject(StreetMode.WALK, Map.of(), FROM);

    @Test
    void theCostOfAVeryLongPathWithEdgesIsCapped() {
      // A single edge from Berlin to Boston
      var edge = StreetModelForTest.streetEdge(
        intersectionVertex(Coordinates.BERLIN),
        intersectionVertex(Coordinates.BOSTON)
      );
      var path = new NearbyStop(PLAIN, edge.getDistanceMeters(), List.of(edge), null);

      assertThat(criteria(path).c1()).isEqualTo(MAX_RAPTOR_C1);
    }

    @Test
    void theCostOfAShortPathWithEdgesIsNotCapped() {
      var edge = StreetModelForTest.streetEdge(
        intersectionVertex(Coordinates.BERLIN),
        intersectionVertex(Coordinates.BERLIN_BRANDENBURG_GATE)
      );
      var path = new NearbyStop(PLAIN, edge.getDistanceMeters(), List.of(edge), null);

      assertThat(criteria(path).c1()).isLessThan(MAX_RAPTOR_C1);
    }

    @Test
    void theCostOfAVeryLongPathWithoutEdgesIsCapped() {
      assertThat(criteria(withoutEdges(8_000_000)).c1()).isEqualTo(MAX_RAPTOR_C1);
    }

    @Test
    void theCostOfAPathWithoutEdgesDoesNotOverflow() {
      assertThat(criteria(withoutEdges(Integer.MAX_VALUE)).c1()).isEqualTo(MAX_RAPTOR_C1);
    }

    @Test
    void aNegativeCostOfAPathWithoutEdgesIsCapped() {
      assertThat(criteria(withoutEdges(-5)).c1()).isEqualTo(MAX_RAPTOR_C1);
    }

    @Test
    void theCostOfAShortPathWithoutEdgesIsNotCapped() {
      assertThat(criteria(withoutEdges(200)).c1()).isLessThan(MAX_RAPTOR_C1);
    }

    private PathCriteria criteria(NearbyStop path) {
      return subject.computePathCriteria(path).orElseThrow();
    }

    private static NearbyStop withoutEdges(double distanceMeters) {
      return new NearbyStop(PLAIN, distanceMeters, List.of(), null);
    }
  }

  private DefaultTransferSearch subject(
    StreetMode mode,
    Map<FeedScopedId, Set<AllowedMode>> modes,
    FeedScopedId fromStop
  ) {
    return subject(mode, modes, fromStop, stopId -> true, stopId -> true);
  }

  private DefaultTransferSearch subject(
    StreetMode mode,
    Map<FeedScopedId, Set<AllowedMode>> modes,
    FeedScopedId fromStop,
    Predicate<FeedScopedId> isSource,
    Predicate<FeedScopedId> isTarget
  ) {
    var graph = new Graph();
    graph.addVertex(
      TransitStopVertexBuilderFactory.ofStop(
        Integer.parseInt(fromStop.getId()),
        new Coordinate(10.0, 59.0)
      ).build()
    );
    graph.index();
    AllowedModesLookup lookup = stopId -> modes.getOrDefault(stopId, Set.of());
    var request = RouteRequest.defaultValue()
      .copyOf()
      .withJourney(jb -> jb.withTransfer(mode))
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(1.0)).withBike(b -> b.withSpeed(1.0)))
      .buildDefault();
    return new DefaultTransferSearch(graph, finder, isSource, isTarget, lookup, request);
  }

  private static List<FeedScopedId> stopIds(Collection<TransferPath<NearbyStop>> paths) {
    return paths.stream().map(TransferPath::toStop).toList();
  }

  private static int minutes(int minutes) {
    return minutes * 60;
  }

  /** Returns the stops it is given, with a distance in meters that is the duration at 1 m/s. */
  private static class FakeNearbyStopFinder implements NearbyStopFinder {

    private final List<NearbyStop> stops = new ArrayList<>();
    private int calls = 0;
    private Duration durationLimit;

    FakeNearbyStopFinder add(FeedScopedId stopId, int durationSeconds) {
      stops.add(new NearbyStop(stopId, durationSeconds, List.of(), null));
      return this;
    }

    @Override
    public Collection<NearbyStop> findNearbyStops(
      Vertex vertex,
      RouteRequest routingRequest,
      StreetMode streetMode,
      boolean reverseDirection,
      Duration durationLimit,
      int maxStopCount
    ) {
      calls++;
      this.durationLimit = durationLimit;
      return stops;
    }

    @Override
    public List<NearbyStop> findNearbyStops(Coordinate coordinate, double radiusMeters) {
      throw new UnsupportedOperationException();
    }
  }
}
