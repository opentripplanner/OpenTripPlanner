package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.routing.api.request.WheelchairAccessibilityFeature.ofOnlyAccessible;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.test.support.VariableSource;

class StreetEdgeCostTest extends GraphRoutingTest {

  StreetVertex V1;
  StreetVertex V2;

  Graph graph;

  public StreetEdgeCostTest() {
    graph =
      graphOf(
        new Builder() {
          @Override
          public void build() {
            V1 = intersection("V1", 0.0, 0.0);
            V2 = intersection("V2", 2.0, 0.0);
          }
        }
      );
  }

  static Stream<Arguments> walkReluctanceCases = Stream.of(
    Arguments.of(0.5, 37),
    Arguments.of(1, 75),
    Arguments.of(2, 150),
    Arguments.of(3, 225)
  );

  @ParameterizedTest(name = "walkRelucance of {0} should lead to traversal costs of {1}")
  @VariableSource("walkReluctanceCases")
  public void shouldApplyWalkReluctance(double walkReluctance, long expectedCost) {
    double length = 100;
    var edge = new StreetEdge(V1, V2, null, "edge", length, StreetTraversalPermission.ALL, false);

    var req = new RoutingRequest();
    req.setWalkReluctance(walkReluctance);
    State result = traverse(edge, req);
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);
  }

  private State traverse(StreetEdge edge, RoutingRequest req) {
    var ctx = new RoutingContext(req, graph, V1, V2);
    var state = new State(ctx);

    assertEquals(0, state.weight);
    return edge.traverse(state);
  }
}
