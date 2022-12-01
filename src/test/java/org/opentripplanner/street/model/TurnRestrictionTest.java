package org.opentripplanner.street.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;

public class TurnRestrictionTest {

  private Graph graph;

  private Vertex topRight;

  private Vertex bottomLeft;

  private StreetEdge maple_main1, broad1_2;

  @BeforeEach
  public void before() {
    graph = new Graph();

    // Graph for a fictional grid city with turn restrictions
    StreetVertex maple1 = vertex("maple_1st", 2.0, 2.0);
    StreetVertex maple2 = vertex("maple_2nd", 1.0, 2.0);
    StreetVertex maple3 = vertex("maple_3rd", 0.0, 2.0);

    StreetVertex main1 = vertex("main_1st", 2.0, 1.0);
    StreetVertex main2 = vertex("main_2nd", 1.0, 1.0);
    StreetVertex main3 = vertex("main_3rd", 0.0, 1.0);

    StreetVertex broad1 = vertex("broad_1st", 2.0, 0.0);
    StreetVertex broad2 = vertex("broad_2nd", 1.0, 0.0);
    StreetVertex broad3 = vertex("broad_3rd", 0.0, 0.0);

    // Each block along the main streets has unit length and is one-way
    StreetEdge maple1_2 = edge(maple1, maple2, 100.0, false);
    StreetEdge maple2_3 = edge(maple2, maple3, 100.0, false);

    StreetEdge main1_2 = edge(main1, main2, 100.0, false);
    StreetEdge main2_3 = edge(main2, main3, 100.0, false);

    broad1_2 = edge(broad1, broad2, 100.0, false);
    StreetEdge broad2_3 = edge(broad2, broad3, 100.0, false);

    // Each cross-street connects
    maple_main1 = edge(maple1, main1, 50.0, false);
    StreetEdge main_broad1 = edge(main1, broad1, 100.0, false);

    StreetEdge maple_main2 = edge(maple2, main2, 50.0, false);
    StreetEdge main_broad2 = edge(main2, broad2, 50.0, false);

    StreetEdge maple_main3 = edge(maple3, main3, 100.0, false);
    StreetEdge main_broad3 = edge(main3, broad3, 100.0, false);

    // Turn restrictions are only for driving modes.
    // - can't turn from 1st onto Main.
    // - can't turn from 2nd onto Main.
    // - can't turn from 2nd onto Broad.
    DisallowTurn(maple_main1, main1_2);
    DisallowTurn(maple_main2, main2_3);
    DisallowTurn(main_broad2, broad2_3);

    // Hold onto some vertices for the tests
    topRight = maple1;
    bottomLeft = broad3;
  }

  @Test
  public void testHasExplicitTurnRestrictions() {
    assertFalse(maple_main1.getTurnRestrictions().isEmpty());
    assertTrue(broad1_2.getTurnRestrictions().isEmpty());
  }

  @Test
  public void testForwardDefault() {
    var request = new RouteRequest();

    request.withPreferences(preferences ->
      preferences.withCar(it -> it.withSpeed(1.0)).withWalk(w -> w.withSpeed(1.0))
    );

    ShortestPathTree<State, Edge, Vertex> tree = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setFrom(topRight)
      .setTo(bottomLeft)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = tree.getPath(bottomLeft);
    assertNotNull(path);

    // Since there are no turn restrictions applied to the default modes (walking + transit)
    // the shortest path is 1st to Main, Main to 2nd, 2nd to Broad and Broad until the
    // corner of Broad and 3rd.

    List<State> states = path.states;
    assertEquals(5, states.size());

    assertEquals("maple_1st", states.get(0).getVertex().getLabel());
    assertEquals("main_1st", states.get(1).getVertex().getLabel());
    assertEquals("main_2nd", states.get(2).getVertex().getLabel());
    assertEquals("broad_2nd", states.get(3).getVertex().getLabel());
    assertEquals("broad_3rd", states.get(4).getVertex().getLabel());
  }

  @Test
  public void testForwardAsPedestrian() {
    var request = new RouteRequest();
    request.withPreferences(pref -> pref.withWalk(w -> w.withSpeed(1.0)));

    ShortestPathTree<State, Edge, Vertex> tree = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setFrom(topRight)
      .setTo(bottomLeft)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = tree.getPath(bottomLeft);
    assertNotNull(path);

    // Since there are no turn restrictions applied to the default modes (walking + transit)
    // the shortest path is 1st to Main, Main to 2nd, 2nd to Broad and Broad until the
    // corner of Broad and 3rd.

    List<State> states = path.states;
    assertEquals(5, states.size());

    assertEquals("maple_1st", states.get(0).getVertex().getLabel());
    assertEquals("main_1st", states.get(1).getVertex().getLabel());
    assertEquals("main_2nd", states.get(2).getVertex().getLabel());
    assertEquals("broad_2nd", states.get(3).getVertex().getLabel());
    assertEquals("broad_3rd", states.get(4).getVertex().getLabel());
  }

  @Test
  public void testForwardAsCar() {
    var request = new RouteRequest();
    request.withPreferences(p -> p.withCar(it -> it.withSpeed(1.0)));

    ShortestPathTree<State, Edge, Vertex> tree = StreetSearchBuilder
      .of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setStreetRequest(new StreetRequest(StreetMode.CAR))
      .setFrom(topRight)
      .setTo(bottomLeft)
      .getShortestPathTree();

    GraphPath<State, Edge, Vertex> path = tree.getPath(bottomLeft);
    assertNotNull(path);

    // If not for turn restrictions, the shortest path would be to take 1st to Main,
    // Main to 2nd, 2nd to Broad and Broad until the corner of Broad and 3rd.
    // However, most of these turns are not allowed. Instead, the shortest allowed
    // path is 1st to Broad, Broad to 3rd.

    List<State> states = path.states;
    assertEquals(5, states.size());

    assertEquals("maple_1st", states.get(0).getVertex().getLabel());
    assertEquals("main_1st", states.get(1).getVertex().getLabel());
    assertEquals("broad_1st", states.get(2).getVertex().getLabel());
    assertEquals("broad_2nd", states.get(3).getVertex().getLabel());
    assertEquals("broad_3rd", states.get(4).getVertex().getLabel());
  }

  /****
   * Private Methods
   ****/

  private StreetVertex vertex(String label, double lat, double lon) {
    return new IntersectionVertex(graph, label, lat, lon);
  }

  /**
   * Create an edge. If twoWay, create two edges (back and forth).
   *
   * @param back true if this is a reverse edge
   */
  private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length, boolean back) {
    String labelA = vA.getLabel();
    String labelB = vB.getLabel();
    String name = String.format("%s_%s", labelA, labelB);
    Coordinate[] coords = new Coordinate[2];
    coords[0] = vA.getCoordinate();
    coords[1] = vB.getCoordinate();
    LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

    StreetTraversalPermission perm = StreetTraversalPermission.ALL;
    return new StreetEdge(vA, vB, geom, name, length, perm, back);
  }

  private void DisallowTurn(StreetEdge from, StreetEdge to) {
    TurnRestrictionType rType = TurnRestrictionType.NO_TURN;
    TraverseModeSet restrictedModes = new TraverseModeSet(TraverseMode.CAR);
    TurnRestriction restrict = new TurnRestriction(from, to, rType, restrictedModes, null);
    from.addTurnRestriction(restrict);
  }
}
