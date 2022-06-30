package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.test.support.VariableSource;

class StreetEdgeCostTest extends GraphRoutingTest {

  StreetVertex V1;
  StreetVertex V2;

  Graph graph;

  public StreetEdgeCostTest() {
    var otpModel = graphOf(
      new Builder() {
        @Override
        public void build() {
          V1 = intersection("V1", 0.0, 0.0);
          V2 = intersection("V2", 2.0, 0.0);
        }
      }
    );
    graph = otpModel.graph;
  }

  static Stream<Arguments> walkReluctanceCases = Stream.of(
    Arguments.of(0.5, 37),
    Arguments.of(1, 75),
    Arguments.of(2, 150),
    Arguments.of(3, 225)
  );

  @ParameterizedTest(name = "walkRelucance of {0} should lead to traversal costs of {1}")
  @VariableSource("walkReluctanceCases")
  public void walkReluctance(double walkReluctance, long expectedCost) {
    double length = 100;
    var edge = new StreetEdge(V1, V2, null, "edge", length, StreetTraversalPermission.ALL, false);

    var req = new RoutingRequest();
    req.setWalkReluctance(walkReluctance);
    State result = traverse(edge, req);
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(76, result.getElapsedTimeSeconds());
  }

  static Stream<Arguments> bikeReluctanceCases = Stream.of(
    Arguments.of(0.5, 10),
    Arguments.of(1, 20),
    Arguments.of(2, 40),
    Arguments.of(3, 60)
  );

  @ParameterizedTest(name = "bikeReluctance of {0} should lead to traversal costs of {1}")
  @VariableSource("bikeReluctanceCases")
  public void bikeReluctance(double bikeReluctance, long expectedCost) {
    double length = 100;
    var edge = new StreetEdge(V1, V2, null, "edge", length, StreetTraversalPermission.ALL, false);

    var req = new RoutingRequest();
    req.setMode(TraverseMode.BICYCLE);
    req.setBikeReluctance(bikeReluctance);

    State result = traverse(edge, req);
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(20, result.getElapsedTimeSeconds());
  }

  static Stream<Arguments> carReluctanceCases = Stream.of(
    Arguments.of(0.5, 4),
    Arguments.of(1, 8),
    Arguments.of(2, 17),
    Arguments.of(3, 26)
  );

  @ParameterizedTest(name = "carReluctance of {0} should lead to traversal costs of {1}")
  @VariableSource("carReluctanceCases")
  public void carReluctance(double carReluctance, long expectedCost) {
    double length = 100;
    var edge = new StreetEdge(V1, V2, null, "edge", length, StreetTraversalPermission.ALL, false);

    var req = new RoutingRequest();
    req.setMode(TraverseMode.CAR);
    req.setCarReluctance(carReluctance);

    State result = traverse(edge, req);
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(9, result.getElapsedTimeSeconds());
  }

  static Stream<Arguments> stairsCases = Stream.of(
    Arguments.of(1, 22),
    Arguments.of(1.5, 33),
    Arguments.of(3, 67)
  );

  @ParameterizedTest(name = "stairs reluctance of of {0} should lead to traversal costs of {1}")
  @VariableSource("stairsCases")
  public void stairsReluctance(double stairsReluctance, long expectedCost) {
    double length = 10;
    var edge = new StreetEdge(V1, V2, null, "stairs", length, StreetTraversalPermission.ALL, false);
    edge.setStairs(true);

    var req = new RoutingRequest();
    req.stairsReluctance = stairsReluctance;

    var result = traverse(edge, req);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(23, result.getElapsedTimeSeconds());

    edge.setStairs(false);
    var notStairsResult = traverse(edge, req);
    assertEquals(15, (long) notStairsResult.weight);
  }

  private State traverse(StreetEdge edge, RoutingRequest req) {
    var ctx = new RoutingContext(req, graph, V1, V2);
    var state = new State(ctx);

    assertEquals(0, state.weight);
    return edge.traverse(state);
  }
}
