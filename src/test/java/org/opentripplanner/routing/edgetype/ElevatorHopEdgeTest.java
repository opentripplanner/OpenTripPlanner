package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.model.WheelchairBoarding;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityFeature;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.SimpleVertex;
import org.opentripplanner.test.support.VariableSource;

class ElevatorHopEdgeTest {

  Graph graph = new Graph();
  Vertex from = new SimpleVertex(graph, "from", 0, 0);
  Vertex to = new SimpleVertex(graph, "to", 0, 0);

  static Stream<Arguments> noTraverse = Stream
    .of(WheelchairBoarding.NO_INFORMATION, WheelchairBoarding.NOT_POSSIBLE)
    .map(Arguments::of);

  @ParameterizedTest(name = "{0} should be allowed to traverse when requesting onlyAccessible")
  @VariableSource("noTraverse")
  public void shouldNotTraverse(WheelchairBoarding wheelchairBoarding) {
    var req = new RoutingRequest();
    WheelchairAccessibilityFeature feature = WheelchairAccessibilityFeature.ofOnlyAccessible();
    req.wheelchairAccessibility =
      new WheelchairAccessibilityRequest(true, feature, feature, feature, 25, 8, 10, 25);
    State result = traverse(wheelchairBoarding, req);
    assertNull(result);
  }

  static Stream<Arguments> all = Stream.of(
    Arguments.of(WheelchairBoarding.POSSIBLE, 20), // no extra cost
    Arguments.of(WheelchairBoarding.NO_INFORMATION, 40), // low extra cost
    Arguments.of(WheelchairBoarding.NOT_POSSIBLE, 3620) // high extra cost
  );

  @ParameterizedTest(name = "{0} should allowed to traverse with a cost of {1}")
  @VariableSource("all")
  public void allowByDefault(WheelchairBoarding wheelchairBoarding, double expectedCost) {
    var req = new RoutingRequest();
    var result = traverse(wheelchairBoarding, req);
    assertNotNull(result);
    assertTrue(result.weight > 1);

    req.wheelchairAccessibility = WheelchairAccessibilityRequest.DEFAULT.withEnabled(true);
    var wheelchairResult = traverse(wheelchairBoarding, req);
    assertNotNull(wheelchairResult);
    assertEquals(expectedCost, wheelchairResult.weight);
  }

  private State traverse(WheelchairBoarding wheelchairBoarding, RoutingRequest req) {
    var edge = new ElevatorHopEdge(from, to, StreetTraversalPermission.ALL, wheelchairBoarding);
    var ctx = new RoutingContext(req, graph, from, to);
    var state = new State(ctx);

    return edge.traverse(state);
  }
}
