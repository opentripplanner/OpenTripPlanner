package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.StreetMode.BIKE;
import static org.opentripplanner.routing.api.request.StreetMode.CAR;
import static org.opentripplanner.test.support.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.graph_builder.module.linking.TestVertexLinker;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.RouteRequestBuilder;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.test.support.ResourceLoader;

public class BarrierRoutingTest {

  private static final Instant dateTime = Instant.now();

  private static Graph graph;

  @BeforeAll
  public static void createGraph() {
    TestOtpModel model = ConstantsForTests.buildOsmGraph(
      ResourceLoader.of(BarrierRoutingTest.class).file("herrenberg-barrier-gates.osm.pbf")
    );
    graph = model.graph();
    graph.index();
  }

  /**
   * Access restrictions on nodes should be taken into account, with walking the bike if needed.
   */
  @Test
  public void shouldWalkForBarriers() {
    var from = GenericLocation.fromCoordinate(48.59384, 8.86848);
    var to = GenericLocation.fromCoordinate(48.59370, 8.87079);

    // This takes a detour to avoid walking with the bike
    var polyline1 = computePolyline(graph, from, to, BIKE);
    assertThatPolylinesAreEqual(polyline1, "o~qgH_ccu@DGFENQZ]NOLOHMFKFILB`BOGo@AeD]U}BaA]Q??");

    // The reluctance for walking with the bike is reduced, so a detour is not taken
    var polyline2 = computePolyline(
      graph,
      from,
      to,
      BIKE,
      rr ->
        rr.withPreferences(p ->
          p.withBike(it -> it.withWalking(walking -> walking.withReluctance(1d)))
        ),
      itineraries ->
        itineraries
          .stream()
          .flatMap(i ->
            Stream.of(
              () -> assertEquals(1, i.legs().size()),
              () -> assertEquals(TraverseMode.BICYCLE, i.streetLeg(0).getMode()),
              () ->
                assertEquals(
                  List.of(false, true, false, true, false),
                  i
                    .legs()
                    .get(0)
                    .listWalkSteps()
                    .stream()
                    .map(WalkStep::isWalkingBike)
                    .collect(Collectors.toList())
                )
            )
          )
    );
    assertThatPolylinesAreEqual(polyline2, "o~qgH_ccu@Bi@Bk@Bi@Bg@NaA@_@Dm@Dq@a@KJy@@I@M@E??");
  }

  /**
   * Car-only barriers should be driven around.
   */
  @Test
  public void shouldDriveAroundBarriers() {
    var from = GenericLocation.fromCoordinate(48.59291, 8.87037);
    var to = GenericLocation.fromCoordinate(48.59262, 8.86879);

    // This takes a detour to avoid walking with the bike
    var polyline1 = computePolyline(graph, from, to, CAR);
    assertThatPolylinesAreEqual(polyline1, "sxqgHyncu@ZTnAFRdEyAFPpA");
  }

  @Test
  public void shouldDriveToBarrier() {
    var from = GenericLocation.fromCoordinate(48.59291, 8.87037);
    var to = GenericLocation.fromCoordinate(48.59276, 8.86963);

    // This takes a detour to avoid walking with the bike
    var polyline1 = computePolyline(graph, from, to, CAR);
    assertThatPolylinesAreEqual(polyline1, "sxqgHyncu@ZT?~B");
  }

  @Test
  public void shouldDriveFromBarrier() {
    var from = GenericLocation.fromCoordinate(48.59273, 8.86931);
    var to = GenericLocation.fromCoordinate(48.59291, 8.87037);

    // This takes a detour to avoid walking with the bike
    var polyline1 = computePolyline(graph, from, to, CAR);
    assertThatPolylinesAreEqual(polyline1, "qwqgHchcu@BTxAGSeEoAG[U");
  }

  private static String computePolyline(
    Graph graph,
    GenericLocation from,
    GenericLocation to,
    StreetMode streetMode
  ) {
    return computePolyline(
      graph,
      from,
      to,
      streetMode,
      ignored -> {},
      itineraries ->
        itineraries
          .stream()
          .flatMap(i -> i.legs().stream())
          .map(
            l ->
              () ->
                assertEquals(
                  mapMode(streetMode),
                  (l instanceof StreetLeg s) ? s.getMode() : null,
                  "Allow only " + streetMode + " legs"
                )
          )
    );
  }

  private static TraverseMode mapMode(StreetMode streetMode) {
    return switch (streetMode) {
      case WALK -> TraverseMode.WALK;
      case BIKE -> TraverseMode.BICYCLE;
      case CAR -> TraverseMode.CAR;
      default -> throw new IllegalArgumentException();
    };
  }

  private static String computePolyline(
    Graph graph,
    GenericLocation from,
    GenericLocation to,
    StreetMode streetMode,
    Consumer<RouteRequestBuilder> options,
    Function<List<Itinerary>, Stream<Executable>> assertions
  ) {
    var builder = RouteRequest.of()
      .withDateTime(dateTime)
      .withFrom(from)
      .withTo(to)
      .withJourney(jb -> jb.withDirect(new StreetRequest(streetMode)));

    options.accept(builder);

    var temporaryVertices = new TemporaryVerticesContainer(
      graph,
      TestVertexLinker.of(graph),
      id -> Set.of(),
      from,
      to,
      streetMode,
      streetMode
    );
    var gpf = new GraphPathFinder(null);
    var paths = gpf.graphPathFinderEntryPoint(builder.buildRequest(), temporaryVertices);

    GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
      ZoneIds.BERLIN,
      graph.streetNotesService,
      graph.ellipsoidToGeoidDifference
    );

    var itineraries = graphPathToItineraryMapper.mapItineraries(paths);

    assertAll(assertions.apply(itineraries));

    Geometry legGeometry = itineraries.get(0).legs().get(0).legGeometry();
    temporaryVertices.close();

    return EncodedPolyline.encode(legGeometry).points();
  }
}
