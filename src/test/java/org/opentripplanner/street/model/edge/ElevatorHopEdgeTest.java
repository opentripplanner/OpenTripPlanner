package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.SimpleVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.basic.Accessibility;

class ElevatorHopEdgeTest {

  Graph graph = new Graph();
  Vertex from = new SimpleVertex(graph, "from", 0, 0);
  Vertex to = new SimpleVertex(graph, "to", 0, 0);

  static Stream<Arguments> noTraverse = Stream
    .of(Accessibility.NO_INFORMATION, Accessibility.NOT_POSSIBLE)
    .map(Arguments::of);

  @ParameterizedTest(name = "{0} should be allowed to traverse when requesting onlyAccessible")
  @VariableSource("noTraverse")
  public void shouldNotTraverse(Accessibility wheelchair) {
    var req = StreetSearchRequest.of();
    AccessibilityPreferences feature = AccessibilityPreferences.ofOnlyAccessible();
    req
      .withWheelchair(true)
      .withPreferences(preferences ->
        preferences.withWheelchair(
          new WheelchairPreferences(feature, feature, feature, 25, 0.5, 10, 25)
        )
      );

    State result = traverse(wheelchair, req.build());
    assertNull(result);
  }

  static Stream<Arguments> all = Stream.of(
    // no extra cost
    Arguments.of(Accessibility.POSSIBLE, 20),
    // low extra cost
    Arguments.of(Accessibility.NO_INFORMATION, 40),
    // high extra cost
    Arguments.of(Accessibility.NOT_POSSIBLE, 3620)
  );

  @ParameterizedTest(name = "{0} should allowed to traverse with a cost of {1}")
  @VariableSource("all")
  public void allowByDefault(Accessibility wheelchair, double expectedCost) {
    var req = StreetSearchRequest.of().build();
    var result = traverse(wheelchair, req);
    assertNotNull(result);
    assertTrue(result.weight > 1);

    req = StreetSearchRequest.copyOf(req).withWheelchair(true).build();
    var wheelchairResult = traverse(wheelchair, req);
    assertNotNull(wheelchairResult);
    assertEquals(expectedCost, wheelchairResult.weight);
  }

  private State traverse(Accessibility wheelchair, StreetSearchRequest req) {
    var edge = new ElevatorHopEdge(from, to, StreetTraversalPermission.ALL, wheelchair);
    var state = new State(from, req);

    return edge.traverse(state);
  }
}
