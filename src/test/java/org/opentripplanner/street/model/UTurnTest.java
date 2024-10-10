package org.opentripplanner.street.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.LinkingDirection;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.edge.StreetTransitStopLink;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.transit.model._data.TransitModelForTest;

public class UTurnTest {

  private Graph graph;
  private Vertex topRight;

  private Vertex topLeft;

  private StreetEdge maple_main1, main_broad1;

  /*
  This test constructs a simplified graph to test u turn avoidance.
  Note: the coordinates are smaller than for other tests, as their distance is
  important, especially for isCloseToStartOrEnd checks of the dominance function.

   b1 <--100-- ma1 <--100-- mp1
   ^            ^            I
   100         100          100
   I            v            v
   b2 <--300-- ma2 <--800-- mp2

   */
  @BeforeEach
  public void before() {
    graph = new Graph();
    // Graph for a fictional grid city with turn restrictions
    StreetVertex maple1 = intersectionVertex("maple_1st", 0.002, 0.002);
    graph.addVertex(maple1);
    StreetVertex maple2 = intersectionVertex("maple_2nd", 0.001, 0.002);
    graph.addVertex(maple2);

    StreetVertex main1 = intersectionVertex("main_1st", 0.002, 0.001);
    graph.addVertex(main1);
    StreetVertex main2 = intersectionVertex("main_2nd", 0.001, 0.001);
    graph.addVertex(main2);
    StreetVertex broad1 = intersectionVertex("broad_1st", 0.002, 0.0);
    graph.addVertex(broad1);
    StreetVertex broad2 = intersectionVertex("broad_2nd", 0.001, 0.0);
    graph.addVertex(broad2);

    // Each block along the main streets has unit length and is one-way
    StreetEdge maple1_2 = edge(maple1, maple2, 100.0, false);
    StreetEdge main1_2 = edge(main1, main2, 100.0, false);
    StreetEdge main2_1 = edge(main2, main1, 100.0, true);
    StreetEdge broad2_1 = edge(broad2, broad1, 100.0, false);

    // Each cross-street connects
    maple_main1 = edge(maple1, main1, 100.0, false);
    main_broad1 = edge(main1, broad1, 100.0, false);

    StreetEdge maple_main2 = edge(maple2, main2, 800.0, false);
    StreetEdge main_broad2 = edge(main2, broad2, 300.0, false);

    graph.index(null);
    // Hold onto some vertices for the tests
    topRight = maple1;
    topLeft = broad1;
  }

  @Test
  public void testDefault() {
    GraphPath<State, Edge, Vertex> path = getPath();

    // The shortest path is 1st to Main, Main to Broad, 1st to 2nd.

    assertVertexSequence(path, new String[] { "maple_1st", "main_1st", "broad_1st" });
  }

  @Test
  public void testNoUTurn() {
    DisallowTurn(maple_main1, main_broad1);

    GraphPath<State, Edge, Vertex> path = getPath();

    // Since there is a turn restrictions applied car mode,
    // the shortest path is 1st to Main, Main to 2nd, 2nd to Broad.
    // U turns usually are prevented by StreetEdge.doTraverse's isReversed check and
    // the dominanceFunction which usually prevents that the same vertex is visited multiple times
    // with the same mode.

    assertVertexSequence(
      path,
      new String[] { "maple_1st", "main_1st", "main_2nd", "broad_2nd", "broad_1st" }
    );
  }

  @Test
  public void testNoUTurnWithLinkedStop() {
    DisallowTurn(maple_main1, main_broad1);
    TransitStopVertex stop = TransitStopVertex
      .of()
      .withStop(TransitModelForTest.of().stop("UTurnTest:1234", 0.0015, 0.0011).build())
      .build();

    // Stop linking splits forward and backward edge, currently with to distinct split vertices.
    graph
      .getLinker()
      .linkVertexPermanently(
        stop,
        new TraverseModeSet(TraverseMode.WALK),
        LinkingDirection.BOTH_WAYS,
        (vertex, streetVertex) ->
          List.of(
            StreetTransitStopLink.createStreetTransitStopLink(
              (TransitStopVertex) vertex,
              streetVertex
            ),
            StreetTransitStopLink.createStreetTransitStopLink(
              streetVertex,
              (TransitStopVertex) vertex
            )
          )
      );

    GraphPath<State, Edge, Vertex> path = getPath();

    // Since there is a turn restrictions applied car mode,
    // the shortest path (without u-turn) should be 1st to Main, Main to 2nd, 2nd to Broad, back to 1st.

    assertVertexSequence(
      path,
      new String[] { "maple_1st", "main_1st", "split_", "main_2nd", "broad_2nd", "broad_1st" }
    );
  }

  private GraphPath<State, Edge, Vertex> getPath() {
    var request = new RouteRequest();
    // We set From/To explicitly, so that fromEnvelope/toEnvelope
    request.setFrom(new GenericLocation(topRight.getLat(), topRight.getLon()));
    request.setTo(new GenericLocation(topLeft.getLat(), topLeft.getLon()));

    ShortestPathTree<State, Edge, Vertex> tree = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setStreetRequest(new StreetRequest(StreetMode.CAR))
      // It is necessary to set From/To explicitly, though it is provided via request already
      .setFrom(topRight)
      .setTo(topLeft)
      .getShortestPathTree();

    return tree.getPath(topLeft);
  }

  private void assertVertexSequence(GraphPath<State, Edge, Vertex> path, String[] vertexLabels) {
    assertNotNull(path);
    List<State> states = path.states;
    assertEquals(vertexLabels.length, states.size());

    for (int i = 0; i < vertexLabels.length; i++) {
      // we check via startsWith, as splitting order is not deterministic. In consequence split_0 / split_1 both
      // would be possible names of a visited node.

      String labelString = states.get(i).getVertex().getLabelString();
      assertTrue(
        labelString.startsWith(vertexLabels[i]),
        "state " +
        i +
        " does not match expected state: " +
        labelString +
        " should start with " +
        vertexLabels[i]
      );
    }
  }

  /**
   * Create an edge. If twoWay, create two edges (back and forth).
   *
   * @param back true if this is a reverse edge
   */
  private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length, boolean back) {
    var labelA = vA.getLabel();
    var labelB = vB.getLabel();
    String name = String.format("%s_%s", labelA, labelB);
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    StreetTraversalPermission perm = StreetTraversalPermission.ALL;
    return new StreetEdgeBuilder<>()
      .withFromVertex(vA)
      .withToVertex(vB)
      .withGeometry(geom)
      .withName(name)
      .withMeterLength(length)
      .withPermission(perm)
      .withBack(back)
      .buildAndConnect();
  }

  private void DisallowTurn(StreetEdge from, StreetEdge to) {
    TurnRestrictionType rType = TurnRestrictionType.NO_TURN;
    TraverseModeSet restrictedModes = new TraverseModeSet(TraverseMode.CAR);
    TurnRestriction restrict = new TurnRestriction(from, to, rType, restrictedModes, null);
    from.addTurnRestriction(restrict);
  }
}
