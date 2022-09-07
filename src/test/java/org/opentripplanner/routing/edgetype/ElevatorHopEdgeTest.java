package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.WheelchairAccessibilityFeature;
import org.opentripplanner.routing.api.request.preference.WheelchairAccessibilityPreferences;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.SimpleVertex;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;

class ElevatorHopEdgeTest {

  Graph graph = new Graph();
  Vertex from = new SimpleVertex(graph, "from", 0, 0);
  Vertex to = new SimpleVertex(graph, "to", 0, 0);

  static Stream<Arguments> noTraverse = Stream
    .of(WheelchairAccessibility.NO_INFORMATION, WheelchairAccessibility.NOT_POSSIBLE)
    .map(Arguments::of);

  @ParameterizedTest(name = "{0} should be allowed to traverse when requesting onlyAccessible")
  @VariableSource("noTraverse")
  public void shouldNotTraverse(WheelchairAccessibility wheelchair) {
    var req = new RouteRequest();
    WheelchairAccessibilityFeature feature = WheelchairAccessibilityFeature.ofOnlyAccessible();
    req.setWheelchair(true);
    req
      .preferences()
      .setWheelchairAccessibility(
        new WheelchairAccessibilityPreferences(feature, feature, feature, 25, 8, 10, 25)
      );
    State result = traverse(wheelchair, req);
    assertNull(result);
  }

  static Stream<Arguments> all = Stream.of(
    // no extra cost
    Arguments.of(WheelchairAccessibility.POSSIBLE, 20),
    // low extra cost
    Arguments.of(WheelchairAccessibility.NO_INFORMATION, 40),
    // high extra cost
    Arguments.of(WheelchairAccessibility.NOT_POSSIBLE, 3620)
  );

  @ParameterizedTest(name = "{0} should allowed to traverse with a cost of {1}")
  @VariableSource("all")
  public void allowByDefault(WheelchairAccessibility wheelchair, double expectedCost) {
    var req = new RouteRequest();
    var result = traverse(wheelchair, req);
    assertNotNull(result);
    assertTrue(result.weight > 1);

    req.setWheelchair(true);
    var wheelchairResult = traverse(wheelchair, req);
    assertNotNull(wheelchairResult);
    assertEquals(expectedCost, wheelchairResult.weight);
  }

  private State traverse(WheelchairAccessibility wheelchair, RouteRequest req) {
    var edge = new ElevatorHopEdge(from, to, StreetTraversalPermission.ALL, wheelchair);
    var ctx = new RoutingContext(req, graph, from, to);
    var state = new State(ctx);

    return edge.traverse(state);
  }
}
