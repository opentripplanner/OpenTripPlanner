package org.opentripplanner.street.search.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;
import static org.opentripplanner.street.model._data.StreetModelForTest.streetEdge;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.AStar;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateData;

public class DominanceFunctionTest {

  @Test
  public void testGeneralDominanceFunction() {
    DominanceFunction minimumWeightDominanceFunction = new DominanceFunctions.MinimumWeight();
    Vertex fromVertex = intersectionVertex(1, 1);
    Vertex toVertex = intersectionVertex(2, 2);

    // Test if domination works in the general case

    StreetSearchRequest streetSearchRequest = StreetSearchRequest.of().build();
    StateData stateData = StateData.getBaseCaseStateData(streetSearchRequest);
    State stateA = new State(fromVertex, Instant.EPOCH, stateData, streetSearchRequest);
    State stateB = new State(toVertex, Instant.EPOCH, stateData, streetSearchRequest);
    stateA.weight = 1;
    stateB.weight = 2;

    assertTrue(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateA, stateB));
    assertFalse(minimumWeightDominanceFunction.betterOrEqualAndComparable(stateB, stateA));
  }

  // TODO: Make unit tests for rest of dominance functionality
  // TODO: Make functional tests for concepts covered by dominance with current algorithm
  // (Specific transfers, bike rental, park and ride, turn restrictions)

  @Test
  public void noDropOffZone() {
    var dominanceF = new DominanceFunctions.MinimumWeight();

    var fromVertex = intersectionVertex(1, 1);
    var toVertex = intersectionVertex(2, 2);

    var req = StreetSearchRequest.of().withMode(StreetMode.SCOOTER_RENTAL).build();

    StateData stateData = StateData.getBaseCaseStateData(req);

    State outsideZone = new State(fromVertex, Instant.EPOCH, stateData, req);
    assertFalse(outsideZone.isInsideNoRentalDropOffArea());

    var edge = StreetModelForTest.streetEdge(fromVertex, toVertex);

    var editor = outsideZone.edit(edge);
    editor.enterNoRentalDropOffArea();
    var insideZone = editor.makeState();
    insideZone.weight = 1;

    assertFalse(dominanceF.betterOrEqualAndComparable(insideZone, outsideZone));
    assertFalse(dominanceF.betterOrEqualAndComparable(outsideZone, insideZone));
  }

  @Test
  public void testBestTurnIsTaken() {
    var dominanceF = new DominanceFunctions.MinimumWeight();

    var origin = intersectionVertex(0, 0);
    var intersection = intersectionVertex(0.01, 0);
    var destination = intersectionVertex(0.001, -0.01);

    var geometry1 = GeometryUtils.getGeometryFactory()
      .createLineString(
        new Coordinate[] {
          new Coordinate(origin.getX(), origin.getY()),
          new Coordinate(0.00015, 0.01),
          new Coordinate(intersection.getX(), intersection.getY()),
        }
      );

    // edge1 is slightly longer than edge 2, but edge2 has an against-traffic turn to the outgoing edge
    // which incurs significant penalty on a bike
    var edge1 = StreetModelForTest.streetEdgeBuilder(
      origin,
      intersection,
      SphericalDistanceLibrary.length(geometry1),
      StreetTraversalPermission.ALL
    )
      .withGeometry(geometry1)
      .buildAndConnect();
    var edge2 = streetEdge(origin, intersection);
    var outgoingEdge = streetEdge(intersection, destination);

    var routeRequest = RouteRequest.defaultValue();
    var streetRequest = new StreetRequest(StreetMode.BIKE);
    var req = StreetSearchRequestMapper.map(routeRequest).withMode(StreetMode.BIKE).build();

    // ensure that I am testing the thing I want to test
    State baseState = new State(origin, Instant.EPOCH, StateData.getBaseCaseStateData(req), req);
    State viaEdge1 = edge1.traverse(baseState)[0];
    State viaEdge2 = edge2.traverse(baseState)[0];
    assertTrue(
      viaEdge1.getWeight() > viaEdge2.getWeight(),
      "Precondition failed: weight at intersection via edge 1 must be higher than via edge 2. Please fix the test case."
    );

    State destinationViaEdge1 = outgoingEdge.traverse(viaEdge1)[0];
    State destinationViaEdge2 = outgoingEdge.traverse(viaEdge2)[0];
    assertTrue(
      destinationViaEdge1.getWeight() < destinationViaEdge2.getWeight(),
      "Precondition failed: weight at destination via edge 1 must be lower than via edge 2. Please fix the test case."
    );

    // test that the dominance function behaves as I want
    assertFalse(dominanceF.betterOrEqualAndComparable(viaEdge1, viaEdge2));
    assertFalse(dominanceF.betterOrEqualAndComparable(viaEdge2, viaEdge1));
    assertTrue(dominanceF.betterOrEqualAndComparable(destinationViaEdge1, destinationViaEdge2));

    // test that A* returns the best path
    Graph graph = new Graph();
    graph.addVertex(origin);
    graph.addVertex(intersection);
    graph.addVertex(destination);

    var path = StreetSearchBuilder.of()
      .setRequest(routeRequest)
      .setStreetRequest(streetRequest)
      .setFrom(origin)
      .setTo(destination)
      .setDominanceFunction(dominanceF)
      .getPathsToTarget()
      .getFirst();

    assertEquals(List.of(edge1, outgoingEdge), path.edges);
  }
}
