package org.opentripplanner.street.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.opentripplanner.test.support.PolylineAssert.assertThatPolylinesAreEqual;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.framework.geometry.EncodedPolyline;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.TurnRestriction;
import org.opentripplanner.street.model.TurnRestrictionType;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.test.support.ResourceLoader;

public class CarRoutingTest {

  static final Instant dateTime = Instant.now();
  private static final ResourceLoader RESOURCE_LOADER = ResourceLoader.of(CarRoutingTest.class);

  private static Graph herrenbergGraph;

  @BeforeAll
  public static void setup() {
    TestOtpModel model = ConstantsForTests.buildOsmGraph(
      RESOURCE_LOADER.file("herrenberg-minimal.osm.pbf")
    );
    herrenbergGraph = model.index().graph();
  }

  /**
   * The OTP algorithm tries hard to never visit the same node twice. This is generally a good idea
   * because it avoids useless loops in the traversal leading to way faster processing time.
   * <p>
   * However there is are certain rare pathological cases where through a series of turn
   * restrictions and roadworks you absolutely must visit a vertex twice if you want to produce a
   * result. One example would be a route like this: https://tinyurl.com/ycqux93g (Note: At the time
   * of writing this Hindenburgstr. (https://www.openstreetmap.org/way/415545869) is closed due to
   * roadworks.)
   * <p>
   * This test checks that such a loop is possible.
   * <p>
   * More information: https://github.com/opentripplanner/OpenTripPlanner/issues/3393
   */
  @Test
  @DisplayName("car routes can contain loops (traversing the same edge twice)")
  public void shouldAllowLoopCausedByTurnRestrictions() {
    TestOtpModel model = ConstantsForTests.buildOsmGraph(
      RESOURCE_LOADER.file("herrenberg-hindenburgstr-under-construction.osm.pbf")
    );
    var hindenburgStrUnderConstruction = model.index().graph();

    var gueltsteinerStr = new GenericLocation(48.59386, 8.87088);
    var aufDemGraben = new GenericLocation(48.59487, 8.87133);

    var polyline = computePolyline(hindenburgStrUnderConstruction, gueltsteinerStr, aufDemGraben);

    assertThatPolylinesAreEqual(
      polyline,
      "s~qgH}qcu@[MuAs@[SAm@Ee@AUEi@XEQkBQ?Bz@Dt@Dh@@TGBC@KBSHGx@"
    );
  }

  @Test
  public void shouldRespectGeneralNoThroughTraffic() {
    var mozartStr = new GenericLocation(48.59521, 8.88391);
    var fritzLeharStr = new GenericLocation(48.59460, 8.88291);

    var polyline1 = computePolyline(herrenbergGraph, mozartStr, fritzLeharStr);
    assertThatPolylinesAreEqual(polyline1, "_grgHkcfu@OjBC\\ARGjAKzAfBz@j@n@Rk@E}D");

    var polyline2 = computePolyline(herrenbergGraph, fritzLeharStr, mozartStr);
    assertThatPolylinesAreEqual(polyline2, "gcrgHc}eu@D|DSj@k@o@gB{@J{AFkA@SB]NkB");
  }

  /**
   * Tests that that https://www.openstreetmap.org/way/35097400 is not taken due to
   * motor_vehicle=destination.
   */
  @Test
  public void shouldRespectMotorCarNoThru() {
    var schiessmauer = new GenericLocation(48.59737, 8.86350);
    var zeppelinStr = new GenericLocation(48.59972, 8.86239);

    var polyline1 = computePolyline(herrenbergGraph, schiessmauer, zeppelinStr);
    assertThatPolylinesAreEqual(
      polyline1,
      "otrgH{cbu@v@|D?bAElBEv@Cj@APGAY?YD]Fm@X_@Pw@d@eAn@k@VM@]He@Fo@Bi@??c@?Q@gD?Q?Q@mD?S"
    );

    var polyline2 = computePolyline(herrenbergGraph, zeppelinStr, schiessmauer);
    assertThatPolylinesAreEqual(
      polyline2,
      "ccsgH{|au@?RAlD?P?PAfD?P?b@h@?n@Cd@G\\ILAj@WdAo@v@e@^Ql@Y\\GXEX?F@@QBk@Dw@DmB?cAw@}D"
    );
  }

  @Test
  public void planningFromNoThroughTrafficPlaceTest() {
    var noThroughTrafficPlace = new GenericLocation(48.59634, 8.87020);
    var destination = new GenericLocation(48.59463, 8.87218);

    var polyline1 = computePolyline(herrenbergGraph, noThroughTrafficPlace, destination);
    assertThatPolylinesAreEqual(
      polyline1,
      "corgHkncu@OEYUOMH?J?LINMNMHTDO@YMm@HS`A}BPGRWLYDEt@HJ@b@?Fc@DONm@t@OXCBz@B\\"
    );

    var polyline2 = computePolyline(herrenbergGraph, destination, noThroughTrafficPlace);
    assertThatPolylinesAreEqual(
      polyline2,
      "scrgH_zcu@C]C{@YBu@NOl@ENGb@c@?KAu@IEDMXSVQFaA|BIRLl@AXENIUOLOLMHK?I?NLXTND"
    );
  }

  private static String computePolyline(Graph graph, GenericLocation from, GenericLocation to) {
    RouteRequest request = new RouteRequest();
    request.setDateTime(dateTime);
    request.setFrom(from);
    request.setTo(to);

    request.journey().direct().setMode(StreetMode.CAR);
    var temporaryVertices = new TemporaryVerticesContainer(
      graph,
      from,
      to,
      StreetMode.CAR,
      StreetMode.CAR
    );
    var gpf = new GraphPathFinder(null);
    var paths = gpf.graphPathFinderEntryPoint(request, temporaryVertices);

    GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
      ZoneIds.BERLIN,
      graph.streetNotesService,
      graph.ellipsoidToGeoidDifference
    );

    var itineraries = graphPathToItineraryMapper.mapItineraries(paths);
    temporaryVertices.close();

    // make sure that we only get CAR legs
    itineraries.forEach(i ->
      i
        .legs()
        .forEach(l -> {
          if (l instanceof StreetLeg stLeg) {
            assertEquals(TraverseMode.CAR, stLeg.getMode());
          } else {
            fail("Expected StreetLeg (CAR): " + l);
          }
        })
    );
    Geometry legGeometry = itineraries.get(0).legs().get(0).getLegGeometry();
    return EncodedPolyline.encode(legGeometry).points();
  }

  private StreetVertex vertex(Graph graph, String label, double lat, double lon) {
    var v = StreetModelForTest.intersectionVertex(label, lat, lon);
    graph.addVertex(v);
    return v;
  }

  private StreetEdge streetEdge(StreetVertex a, StreetVertex b, double length) {
    return new StreetEdgeBuilder<>()
      .withFromVertex(a)
      .withToVertex(b)
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .buildAndConnect();
  }

  private StreetEdge[] edges(StreetVertex a, StreetVertex b, double length) {
    return new StreetEdge[] { streetEdge(a, b, length), streetEdge(b, a, length) };
  }

  @Test
  public void turnRestrictedVisitDoesNotBlockSearch() {
    // The costs of the edges are set up so that the search first goes A->B->D before trying
    // A->B->C->D. The test tests that the previous visit of D does not block the proper
    // path A->B->C->D->F.
    var graph = new Graph();
    var A = vertex(graph, "A", 0.0, 0.0);
    var B = vertex(graph, "B", 1.0, 0.0);
    var C = vertex(graph, "C", 1.5, 1.0);
    var D = vertex(graph, "D", 2.0, 0.0);
    var E = vertex(graph, "E", 3.0, 0.0);
    var F = vertex(graph, "F", 2.0, -1.0);
    edges(A, B, 1.0);
    edges(B, C, 1.0);
    var BD = edges(B, D, 1.0);
    edges(C, D, 1.0);
    edges(D, E, 1.0);
    var DF = edges(D, F, 1.0);
    BD[0].addTurnRestriction(
        new TurnRestriction(
          BD[0],
          DF[0],
          TurnRestrictionType.NO_TURN,
          new TraverseModeSet(TraverseMode.CAR),
          null
        )
      );

    var request = new RouteRequest();
    request.setDateTime(dateTime);
    request.journey().direct().setMode(StreetMode.CAR);

    var streetRequest = new StreetRequest(StreetMode.CAR);

    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setStreetRequest(streetRequest)
      .setFrom(A)
      .setTo(F)
      .getShortestPathTree();
    GraphPath<State, Edge, Vertex> path = spt.getPath(F);
    List<State> states = path.states;
    assertEquals(5, states.size());
    assertEquals(states.get(0).getVertex(), A);
    assertEquals(states.get(1).getVertex(), B);
    assertEquals(states.get(2).getVertex(), C);
    assertEquals(states.get(3).getVertex(), D);
    assertEquals(states.get(4).getVertex(), F);
  }
}
