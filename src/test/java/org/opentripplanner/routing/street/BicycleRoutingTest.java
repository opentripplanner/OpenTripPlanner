package org.opentripplanner.routing.street;

import static org.opentripplanner.test.support.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.LegMode;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.core.BicycleOptimizeType;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.util.PolylineEncoder;

public class BicycleRoutingTest {

  static final Instant dateTime = Instant.now();
  private final Graph herrenbergGraph;

  {
    TestOtpModel model = ConstantsForTests.buildOsmGraph(ConstantsForTests.HERRENBERG_OSM);
    herrenbergGraph = model.graph();

    model.transitModel().index();
    herrenbergGraph.index(model.transitModel().getStopModel());
  }

  /**
   * https://www.openstreetmap.org/way/22392895 is access=destination which means that both bicycles
   * and motor vehicles must not pass through.
   */
  @Test
  public void shouldRespectGeneralNoThroughTraffic() {
    var mozartStr = new GenericLocation(48.59713, 8.86107);
    var fritzLeharStr = new GenericLocation(48.59696, 8.85806);

    var polyline1 = computePolyline(herrenbergGraph, mozartStr, fritzLeharStr);
    assertThatPolylinesAreEqual(polyline1, "_srgHutau@h@B|@Jf@BdAG?\\JT@jA?DSp@_@fFsAT{@DBpC");

    var polyline2 = computePolyline(herrenbergGraph, fritzLeharStr, mozartStr);
    assertThatPolylinesAreEqual(polyline2, "{qrgH{aau@CqCz@ErAU^gFRq@?EAkAKUeACg@A_AM_AEDQF@H?");
  }

  /**
   * Tests that https://www.openstreetmap.org/way/35097400 is allowed for cars due to
   * motor_vehicle=destination being meant for cars only.
   */
  @Test
  public void shouldNotRespectMotorCarNoThru() {
    var schiessmauer = new GenericLocation(48.59737, 8.86350);
    var zeppelinStr = new GenericLocation(48.59972, 8.86239);

    var polyline1 = computePolyline(herrenbergGraph, schiessmauer, zeppelinStr);
    assertThatPolylinesAreEqual(polyline1, "otrgH{cbu@S_AU_AmAdAyApAGDs@h@_@\\_ClBe@^?S");

    var polyline2 = computePolyline(herrenbergGraph, zeppelinStr, schiessmauer);
    assertThatPolylinesAreEqual(polyline2, "ccsgH{|au@?Rd@_@~BmB^]r@i@FExAqAlAeAT~@R~@");
  }

  private static String computePolyline(Graph graph, GenericLocation from, GenericLocation to) {
    RouteRequest request = new RouteRequest();
    request.setDateTime(dateTime);
    request.setFrom(from);
    request.setTo(to);
    request.preferences().bike().setOptimizeType(BicycleOptimizeType.QUICK);

    request.streetSubRequestModes = new TraverseModeSet(TraverseMode.BICYCLE);
    var temporaryVertices = new TemporaryVerticesContainer(graph, request);
    RoutingContext routingContext = new RoutingContext(request, graph, temporaryVertices);

    var gpf = new GraphPathFinder(null, Duration.ofSeconds(5));
    var paths = gpf.graphPathFinderEntryPoint(routingContext);

    GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
      ZoneId.of("Europe/Berlin"),
      graph.streetNotesService,
      graph.ellipsoidToGeoidDifference
    );

    var itineraries = graphPathToItineraryMapper.mapItineraries(paths);
    temporaryVertices.close();

    // make sure that we only get BICYLE legs
    itineraries.forEach(i ->
      i.getLegs().forEach(l -> Assertions.assertEquals(LegMode.BICYCLE, l.getMode()))
    );
    Geometry legGeometry = itineraries.get(0).getLegs().get(0).getLegGeometry();
    return PolylineEncoder.encodeGeometry(legGeometry).points();
  }
}
