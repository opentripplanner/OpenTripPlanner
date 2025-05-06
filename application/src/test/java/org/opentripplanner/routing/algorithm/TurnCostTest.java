package org.opentripplanner.routing.algorithm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.street.model.TurnRestrictionTest.getParentLabelString;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.graph_builder.module.TurnRestrictionModule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.service.osminfo.OsmInfoGraphBuildRepository;
import org.opentripplanner.service.osminfo.internal.DefaultOsmInfoGraphBuildRepository;
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
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.TraverseModeSet;
import org.opentripplanner.street.search.intersection_model.ConstantIntersectionTraversalCalculator;
import org.opentripplanner.street.search.intersection_model.IntersectionTraversalCalculator;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;

public class TurnCostTest {

  private Vertex topRight;

  private Vertex bottomLeft;

  private RouteRequest proto;

  private IntersectionTraversalCalculator calculator;

  @BeforeEach
  public void before() {
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

    StreetEdge broad1_2 = edge(broad1, broad2, 100.0, false);
    StreetEdge broad2_3 = edge(broad2, broad3, 100.0, false);

    // Each cross-street connects
    StreetEdge maple_main1 = edge(maple1, main1, 50.0, false);
    StreetEdge main_broad1 = edge(main1, broad1, 100.0, false);

    StreetEdge maple_main2 = edge(maple2, main2, 50.0, false);
    StreetEdge main_broad2 = edge(main2, broad2, 50.0, false);

    StreetEdge maple_main3 = edge(maple3, main3, 100.0, false);
    StreetEdge main_broad3 = edge(main3, broad3, 100.0, false);

    var osmInfoGraphBuildRepository = new DefaultOsmInfoGraphBuildRepository();
    // Turn restrictions are only for driving modes.
    // - can't turn from 1st onto Main.
    // - can't turn from 2nd onto Main.
    // - can't turn from 2nd onto Broad.
    DisallowTurn(osmInfoGraphBuildRepository, maple_main1, main1_2);
    DisallowTurn(osmInfoGraphBuildRepository, maple_main2, main2_3);
    DisallowTurn(osmInfoGraphBuildRepository, main_broad2, broad2_3);

    // Hold onto some vertices for the tests
    topRight = maple1;
    bottomLeft = broad3;

    Graph graph = StreetModelForTest.makeGraph(topRight);
    TurnRestrictionModule turnRestrictionModule = new TurnRestrictionModule(
      graph,
      osmInfoGraphBuildRepository
    );
    turnRestrictionModule.buildGraph();

    // Make a prototype routing request.
    proto = new RouteRequest();
    proto.withPreferences(preferences ->
      preferences
        .withCar(it -> it.withReluctance(1.0))
        .withBike(bike -> bike.withSpeed(1.0).withReluctance(1.0))
        .withScooter(scooter -> scooter.withSpeed(1.0).withReluctance(1.0))
        .withWalk(walk -> walk.withSpeed(1.0).withStairsReluctance(1.0).withReluctance(1.0))
        .withStreet(it -> it.withTurnReluctance(1.0))
    );

    // Turn costs are all 0 by default.
    calculator = new ConstantIntersectionTraversalCalculator(0.0);
  }

  @Test
  public void testForwardDefaultNoTurnCosts() {
    // Without turn costs, this path costs 2x100 + 2x50 = 300.
    checkForwardRouteDuration(proto, StreetMode.WALK, topRight, bottomLeft, 300);
  }

  @Test
  public void testForwardDefaultConstTurnCosts() {
    calculator = new ConstantIntersectionTraversalCalculator(10.0);

    // Without turn costs, this path costs 2x100 + 2x50 = 300.
    // Since we traverse 3 intersections, the total cost should be 330.
    GraphPath<State, Edge, Vertex> path = checkForwardRouteDuration(
      proto,
      StreetMode.WALK,
      topRight,
      bottomLeft,
      330
    );

    // The intersection traversal cost should be applied to the state *after*
    // the intersection itself.
    List<State> states = path.states;
    assertEquals(5, states.size());

    assertEquals("maple_1st", states.get(0).getVertex().getLabelString());
    assertEquals("main_1st", states.get(1).getVertex().getLabelString());
    assertEquals("main_2nd", states.get(2).getVertex().getLabelString());
    assertEquals("broad_2nd", states.get(3).getVertex().getLabelString());
    assertEquals("broad_3rd", states.get(4).getVertex().getLabelString());

    assertEquals(0, states.get(0).getElapsedTimeSeconds());
    assertEquals(50, states.get(1).getElapsedTimeSeconds()); // maple_main1 = 50
    assertEquals(160, states.get(2).getElapsedTimeSeconds()); // main1_2 = 100
    assertEquals(220, states.get(3).getElapsedTimeSeconds()); // main_broad2 = 50
    assertEquals(330, states.get(4).getElapsedTimeSeconds()); // broad2_3 = 100
  }

  @Test
  public void testForwardCarNoTurnCosts() {
    RouteRequest options = proto.clone();

    // Without turn costs, this path costs 3x100 + 1x50 = 300.
    GraphPath<State, Edge, Vertex> path = checkForwardRouteDuration(
      options,
      StreetMode.CAR,
      topRight,
      bottomLeft,
      350
    );

    List<State> states = path.states;
    assertEquals(5, states.size());

    assertEquals("maple_1st", getParentLabelString(states.get(0).getVertex()));
    assertEquals("main_1st", getParentLabelString(states.get(1).getVertex()));
    assertEquals("broad_1st", getParentLabelString(states.get(2).getVertex()));
    assertEquals("broad_2nd", getParentLabelString(states.get(3).getVertex()));
    assertEquals("broad_3rd", getParentLabelString(states.get(4).getVertex()));
  }

  @Test
  public void testForwardCarConstTurnCosts() {
    calculator = new ConstantIntersectionTraversalCalculator(10.0);

    // Without turn costs, this path costs 3x100 + 1x50 = 350.
    // Since there are 3 turns, the total cost should be 380.
    GraphPath<State, Edge, Vertex> path = checkForwardRouteDuration(
      proto,
      StreetMode.CAR,
      topRight,
      bottomLeft,
      380
    );

    List<State> states = path.states;
    assertEquals(5, states.size());

    assertEquals("maple_1st", getParentLabelString(states.get(0).getVertex()));
    assertEquals("main_1st", getParentLabelString(states.get(1).getVertex()));
    assertEquals("broad_1st", getParentLabelString(states.get(2).getVertex()));
    assertEquals("broad_2nd", getParentLabelString(states.get(3).getVertex()));
    assertEquals("broad_3rd", getParentLabelString(states.get(4).getVertex()));

    assertEquals(0, states.get(0).getElapsedTimeSeconds());
    assertEquals(50, states.get(1).getElapsedTimeSeconds()); // maple_main1 = 50
    assertEquals(160, states.get(2).getElapsedTimeSeconds()); // main1_2 = 100
    assertEquals(270, states.get(3).getElapsedTimeSeconds()); // broad1_2 = 100
    assertEquals(380, states.get(4).getElapsedTimeSeconds()); // broad2_3 = 100
  }

  private GraphPath<State, Edge, Vertex> checkForwardRouteDuration(
    RouteRequest request,
    StreetMode streetMode,
    Vertex from,
    Vertex to,
    int expectedDuration
  ) {
    ShortestPathTree<State, Edge, Vertex> tree = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic())
      .setRequest(request)
      .setStreetRequest(new StreetRequest(streetMode))
      .setFrom(from)
      .setTo(to)
      .setIntersectionTraversalCalculator(calculator)
      .getShortestPathTree();
    GraphPath<State, Edge, Vertex> path = tree.getPath(bottomLeft);
    assertNotNull(path);

    // Without turn costs, this path costs 2x100 + 2x50 = 300.
    assertEquals(expectedDuration, path.getDuration());

    // Weight == duration when reluctances == 0.
    assertEquals(expectedDuration, (int) path.getWeight());

    for (State s : path.states) {
      assertEquals(s.getElapsedTimeSeconds(), (int) s.getWeight());
    }

    return path;
  }

  /****
   * Private Methods
   ****/

  private StreetVertex vertex(String label, double lat, double lon) {
    return StreetModelForTest.intersectionVertex(label, lat, lon);
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
      .withCarSpeed(1.0f)
      .buildAndConnect();
  }

  private void DisallowTurn(
    OsmInfoGraphBuildRepository osmInfoGraphBuildRepository,
    StreetEdge from,
    StreetEdge to
  ) {
    TurnRestrictionType rType = TurnRestrictionType.NO_TURN;
    TraverseModeSet restrictedModes = new TraverseModeSet(TraverseMode.CAR);
    TurnRestriction restrict = new TurnRestriction(from, to, rType, restrictedModes);
    osmInfoGraphBuildRepository.addTurnRestriction(restrict);
  }
}
