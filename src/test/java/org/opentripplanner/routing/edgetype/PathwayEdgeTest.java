package org.opentripplanner.routing.edgetype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.WheelchairAccessibilityFeature.ofOnlyAccessible;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.WheelchairAccessibilityRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.SimpleVertex;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.util.NonLocalizedString;

class PathwayEdgeTest {

  Graph graph = new Graph();
  Vertex from = new SimpleVertex(graph, "A", 10, 10);
  Vertex to = new SimpleVertex(graph, "B", 10.001, 10.001);

  @Test
  void zeroLength() {
    // if elevators have a traversal time and distance of 0 we cannot interpolate the distance
    // from the vertices as they most likely have identical coordinates
    var edge = new PathwayEdge(from, to, null, new NonLocalizedString("pathway"), 0, 0, 0, 0, true);

    assertThatEdgeIsTraversable(edge);
  }

  @Test
  void zeroLengthWithSteps() {
    var edge = new PathwayEdge(from, to, null, new NonLocalizedString("pathway"), 0, 0, 2, 0, true);

    assertThatEdgeIsTraversable(edge);
  }

  @Test
  void traversalTime() {
    var edge = new PathwayEdge(
      from,
      to,
      null,
      new NonLocalizedString("pathway"),
      60,
      0,
      0,
      0,
      true
    );

    var state = assertThatEdgeIsTraversable(edge);
    assertEquals(60, state.getElapsedTimeSeconds());
    assertEquals(120, state.getWeight());
  }

  @Test
  void traversalTimeOverridesLength() {
    var edge = new PathwayEdge(
      from,
      to,
      null,
      new NonLocalizedString("pathway"),
      60,
      1000,
      0,
      0,
      true
    );

    assertEquals(1000, edge.getDistanceMeters());

    var state = assertThatEdgeIsTraversable(edge);
    assertEquals(60, state.getElapsedTimeSeconds());
    assertEquals(120, state.getWeight());
  }

  @Test
  void distance() {
    var edge = new PathwayEdge(
      from,
      to,
      null,
      new NonLocalizedString("pathway"),
      0,
      100,
      0,
      0,
      true
    );

    var state = assertThatEdgeIsTraversable(edge);
    assertEquals(133, state.getElapsedTimeSeconds());
    assertEquals(266, state.getWeight());
  }

  @Test
  void wheelchair() {
    var edge = new PathwayEdge(
      from,
      to,
      null,
      new NonLocalizedString("pathway"),
      0,
      100,
      0,
      0,
      false
    );

    var state = assertThatEdgeIsTraversable(edge, true);
    assertEquals(133, state.getElapsedTimeSeconds());
    assertEquals(6650.0, state.getWeight());
  }

  static Stream<Arguments> slopeCases = Stream.of(
    Arguments.of(0.07, 120), // no extra cost
    Arguments.of(0.08, 120), // no extra cost
    Arguments.of(0.09, 1320), // 1 % above max
    Arguments.of(0.091, 1452), // 1.1 % above the max slope, tiny extra cost
    Arguments.of(0.0915, 1518), // 1.15 % above the max slope, will incur larger cost
    Arguments.of(0.11, 3960) // 3 % above max slope, will incur very large cost
  );

  /**
   * This makes sure that when you exceed the max slope in a wheelchair there isn't a hard cut-off
   * but rather the cost increases proportional to how much you go over the maximum.
   * <p>
   * In other words: 0.1 % over the limit only has a small cost but 2% over increases it
   * dramatically to the point where it's only used as a last resort.
   */
  @ParameterizedTest(name = "slope of {0} should lead to traversal costs of {1}")
  @VariableSource("slopeCases")
  public void shouldScaleCostWithMaxSlope(double slope, long expectedCost) {
    var edge = new PathwayEdge(
      from,
      to,
      null,
      new NonLocalizedString("pathway"),
      60,
      100,
      0,
      slope,
      true
    );

    var state = assertThatEdgeIsTraversable(edge, true);
    assertEquals(60, state.getElapsedTimeSeconds());
    assertEquals(expectedCost, (int) state.getWeight());
  }

  private State assertThatEdgeIsTraversable(PathwayEdge edge) {
    return assertThatEdgeIsTraversable(edge, false);
  }

  private State assertThatEdgeIsTraversable(PathwayEdge edge, boolean wheelchair) {
    var req = new RoutingRequest();
    req.wheelchairAccessibility =
      new WheelchairAccessibilityRequest(
        wheelchair,
        ofOnlyAccessible(),
        ofOnlyAccessible(),
        ofOnlyAccessible(),
        25,
        0.08,
        1.1f,
        25
      );
    var state = new State(new RoutingContext(req, graph, from, to));

    var afterTraversal = edge.traverse(state);
    assertNotNull(afterTraversal);

    assertTrue(afterTraversal.getWeight() > 0);
    return afterTraversal;
  }
}
