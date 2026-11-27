package org.opentripplanner.routing.algorithm.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPath;
import org.opentripplanner.model.plan.VertexType;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.leg.ViaLocationType;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.TestVehicleRentalStationBuilder;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetTransitEntranceLink;
import org.opentripplanner.street.model.edge.TemporaryFreeEdge;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.path.step.RelativeDirection;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.TestStateBuilder;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.site.Entrance;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.NoopSiteResolver;

class StreetLegMapperTest {

  /** Start time for paths built by hand and the target time of time-shifting tests. */
  private static final ZonedDateTime PATH_START = OffsetDateTime.parse(
    "2026-11-23T12:00:00+02:00"
  ).atZoneSameInstant(ZoneIds.BERLIN);

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();
  private static final RegularStop STOP_A = TEST_MODEL.stop("A", 1, 1).build();
  private static final RegularStop STOP_B = TEST_MODEL.stop("B", 2, 2).build();
  private static final WgsCoordinate ENTRANCE_COORDINATE = new WgsCoordinate(3, 3);
  private static final Entrance ENTRANCE = Entrance.of(id("entrance"))
    .withCoordinate(ENTRANCE_COORDINATE)
    .build();

  private final StreetLegMapper mapper = new StreetLegMapper(
    new TestSiteResolver(),
    ZoneIds.BERLIN,
    new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
    0
  );

  @Test
  void pathWithoutEdges() {
    var path = new StreetPath(TestStateBuilder.ofWalking().build());
    assertThat(mapper.map(path, RouteRequest.defaultValue())).isEmpty();
  }

  @Test
  void walk() {
    var path = path(TestStateBuilder.ofWalking().streetEdge().streetEdge());

    var legs = mapper.map(path, RouteRequest.defaultValue());

    assertThat(legs).hasSize(1);
    var leg = assertInstanceOf(StreetLeg.class, legs.getFirst());
    assertEquals(TraverseMode.WALK, leg.getMode());
    assertEquals(path.startTime().atZone(ZoneIds.BERLIN), leg.startTime());
    assertEquals(path.endTime().atZone(ZoneIds.BERLIN), leg.endTime());
    assertEquals(path.distanceMeters(), leg.distanceMeters(), 0.01);
    assertTrue(leg.generalizedCost() > 0);
    assertEquals(path.geometry(), leg.legGeometry());
    assertEquals(VertexType.NORMAL, leg.from().vertexType);
    assertEquals(VertexType.NORMAL, leg.to().vertexType);
    assertFalse(leg.rentedVehicle());
    assertNull(leg.vehicleRentalNetwork());
    assertThat(leg.listWalkSteps()).hasSize(2);
  }

  @Test
  void legsAreInConfiguredTimeZone() {
    var path = path(TestStateBuilder.ofWalking().streetEdge());
    var tokyo = ZoneId.of("Asia/Tokyo");
    var tokyoMapper = new StreetLegMapper(
      new TestSiteResolver(),
      tokyo,
      new DefaultStreetDetailsService(new DefaultStreetDetailsRepository()),
      0
    );

    var leg = tokyoMapper.map(path, RouteRequest.defaultValue()).getFirst();

    assertEquals(tokyo, leg.startTime().getZone());
    assertEquals(path.startTime(), leg.startTime().toInstant());
  }

  @Test
  void vehicleRental() {
    var path = path(
      TestStateBuilder.ofScooterRental().streetEdge().pickUpFreeFloatingScooter().streetEdge()
    );

    var legs = mapper.map(path, RouteRequest.defaultValue());

    assertThat(
      legs
        .stream()
        .map(l -> ((StreetLeg) l).getMode())
        .toList()
    )
      .containsExactly(TraverseMode.WALK, TraverseMode.SCOOTER)
      .inOrder();
    var walk = (StreetLeg) legs.get(0);
    assertFalse(walk.rentedVehicle());
    assertNull(walk.vehicleRentalNetwork());

    var scooter = (StreetLeg) legs.get(1);
    assertTrue(scooter.rentedVehicle());
    assertEquals(TestFreeFloatingRentalVehicleBuilder.NETWORK_1, scooter.vehicleRentalNetwork());
    assertEquals(walk.endTime(), scooter.startTime());
  }

  @Test
  void timeShift() {
    var path = path(
      TestStateBuilder.ofScooterRental().streetEdge().pickUpFreeFloatingScooter().streetEdge()
    );
    var unshifted = mapper.map(path, RouteRequest.defaultValue());
    var delay = Duration.between(unshifted.getFirst().startTime(), PATH_START);

    var shifted = mapper.map(path, RouteRequest.defaultValue(), PATH_START);

    assertThat(shifted).hasSize(unshifted.size());
    assertEquals(PATH_START, shifted.getFirst().startTime());
    for (int i = 0; i < shifted.size(); i++) {
      assertEquals(unshifted.get(i).startTime().plus(delay), shifted.get(i).startTime());
      assertEquals(unshifted.get(i).endTime().plus(delay), shifted.get(i).endTime());
    }
  }

  static Stream<Arguments> stopViaCases() {
    return Stream.of(
      argumentSet("no via", List.of(), null),
      argumentSet(
        "visit",
        List.of(new VisitViaLocation(null, null, List.of(STOP_A.getId()), null)),
        ViaLocationType.VISIT
      ),
      argumentSet(
        "pass-through",
        List.of(new PassThroughViaLocation(null, List.of(STOP_A.getId()))),
        ViaLocationType.PASS_THROUGH
      ),
      argumentSet(
        "via at other stop",
        List.of(new VisitViaLocation(null, null, List.of(STOP_B.getId()), null)),
        null
      )
    );
  }

  @ParameterizedTest
  @MethodSource("stopViaCases")
  void stop(List<ViaLocation> viaLocations, ViaLocationType expected) {
    var path = path(TestStateBuilder.ofWalking().streetEdge().stop(STOP_A));

    var leg = mapper.map(path, viaLocations, null).getFirst();

    assertEquals(VertexType.TRANSIT, leg.to().vertexType);
    assertSame(STOP_A, leg.to().stop);
    assertEquals(expected, leg.to().viaLocationType);
  }

  static Stream<Arguments> temporaryLocationViaCases() {
    return Stream.of(
      argumentSet("no via", List.of(), null),
      argumentSet(
        "via at location",
        List.of(new VisitViaLocation(null, null, List.of(), new WgsCoordinate(5, 5))),
        ViaLocationType.VISIT
      ),
      argumentSet(
        "via elsewhere",
        List.of(new VisitViaLocation(null, null, List.of(), new WgsCoordinate(6, 6))),
        null
      )
    );
  }

  @ParameterizedTest
  @MethodSource("temporaryLocationViaCases")
  void temporaryLocation(List<ViaLocation> viaLocations, ViaLocationType expected) {
    var state = TestStateBuilder.ofWalking().streetEdge().build();
    var location = new TemporaryStreetLocation(
      new WgsCoordinate(5, 5).asJtsCoordinate(),
      I18NString.of("destination")
    );
    var edge = TemporaryFreeEdge.createTemporaryFreeEdge(state.getVertex(), location);
    var path = new StreetPath(edge.traverse(state)[0]);

    var leg = mapper.map(path, viaLocations, null).getFirst();

    assertEquals(VertexType.NORMAL, leg.to().vertexType);
    assertEquals(I18NString.of("destination"), leg.to().name);
    assertEquals(new WgsCoordinate(5, 5), leg.to().coordinate);
    assertEquals(expected, leg.to().viaLocationType);
  }

  @Test
  void vehicleRentalStation() {
    var station = TestVehicleRentalStationBuilder.of().build();
    var path = path(TestStateBuilder.ofWalking().streetEdge().rentalStation(station));

    var leg = mapper.map(path, RouteRequest.defaultValue()).getFirst();

    assertEquals(VertexType.VEHICLERENTAL, leg.to().vertexType);
    assertSame(station, leg.to().vehicleRentalPlace);
  }

  @Test
  void stationEntrance() {
    var path = path(TestStateBuilder.ofWalking().streetEdge().entrance("entrance"));

    var step = findStep(path, RelativeDirection.ENTER_OR_EXIT_STATION);

    var entrance = step.entrance().orElseThrow();
    assertEquals("12345", entrance.getId().getId());
    assertEquals("A", entrance.getCode());
    assertEquals(new WgsCoordinate(3, 3), entrance.getCoordinate());
    assertEquals(Accessibility.POSSIBLE, entrance.getWheelchairAccessibility());
  }

  @Test
  void transitEntrance() {
    var state = TestStateBuilder.ofWalking().streetEdge().build();
    var entranceVertex = StreetModelForTest.transitEntranceVertex(
      "entrance",
      ENTRANCE_COORDINATE.latitude(),
      ENTRANCE_COORDINATE.longitude()
    );
    var link = StreetTransitEntranceLink.createStreetTransitEntranceLink(
      (StreetVertex) state.getVertex(),
      entranceVertex
    );
    var path = new StreetPath(link.traverse(state)[0]);

    var steps = mapper.map(path, RouteRequest.defaultValue()).getFirst().listWalkSteps();

    var entrances = steps
      .stream()
      .flatMap(s -> s.entrance().stream())
      .toList();
    assertThat(entrances).containsExactly(ENTRANCE);
    assertSame(ENTRANCE, entrances.getFirst());
  }

  @Test
  void verticalTransportation() {
    var path = path(TestStateBuilder.ofWalking().streetEdge().escalatorEdge().streetEdge());

    var steps = mapper.map(path, RouteRequest.defaultValue()).getFirst().listWalkSteps();

    assertThat(
      steps
        .stream()
        .map(s -> s.verticalTransportationUse().isPresent())
        .toList()
    )
      .containsExactly(false, true, false)
      .inOrder();
  }

  @Test
  void flex() {
    var edge = new FlexTripEdge(
      StreetModelForTest.intersectionVertex(1, 1),
      StreetModelForTest.intersectionVertex(2, 2),
      STOP_A.getId(),
      STOP_B.getId(),
      null,
      1,
      2,
      LocalDate.of(2023, 4, 18),
      new FlexPath(1000, 600, () -> GeometryUtils.makeLineString(1, 1, 2, 2)),
      FlexParameters.defaultValues()
    );
    var path = new StreetPath(edge.traverse(flexState(edge.getFromVertex()))[0]);
    var delay = Duration.ofMinutes(5);

    var legs = mapper.map(path, RouteRequest.defaultValue(), PATH_START.plus(delay));

    assertThat(legs).hasSize(1);
    var leg = assertInstanceOf(FlexibleTransitLeg.class, legs.getFirst());
    assertEquals(edge.getGeometry(), leg.legGeometry());
    assertSame(STOP_A, leg.from().stop);
    assertSame(STOP_B, leg.to().stop);
    assertEquals(PATH_START.plus(delay), leg.startTime());
    assertEquals(PATH_START.plus(delay).plusSeconds(600), leg.endTime());
    assertEquals(path.weight(), leg.generalizedCost(), 1);
  }

  private static StreetPath path(TestStateBuilder builder) {
    return new StreetPath(builder.build());
  }

  private WalkStep findStep(StreetPath path, RelativeDirection direction) {
    return mapper
      .map(path, RouteRequest.defaultValue())
      .getFirst()
      .listWalkSteps()
      .stream()
      .filter(s -> s.getRelativeDirection() == direction)
      .findFirst()
      .orElseThrow();
  }

  private static State flexState(Vertex vertex) {
    return new State(
      vertex,
      StreetSearchRequest.of()
        .withMode(StreetMode.FLEXIBLE)
        .withStartTime(PATH_START.toInstant())
        .build()
    );
  }

  private static class TestSiteResolver extends NoopSiteResolver {

    @Override
    public RegularStop getStop(FeedScopedId id) {
      return (RegularStop) getStopLocation(id);
    }

    @Override
    public StopLocation getStopLocation(FeedScopedId id) {
      return Stream.of(STOP_A, STOP_B)
        .filter(s -> s.getId().equals(id))
        .findFirst()
        .orElseThrow();
    }

    @Override
    public Entrance getEntrance(FeedScopedId id) {
      return ENTRANCE.getId().equals(id) ? ENTRANCE : null;
    }
  }
}
