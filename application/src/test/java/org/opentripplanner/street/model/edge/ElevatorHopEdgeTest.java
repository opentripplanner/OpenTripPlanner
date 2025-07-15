package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.AccessibilityPreferences;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.basic.Accessibility;

class ElevatorHopEdgeTest {

  Vertex from = intersectionVertex(0, 0);
  Vertex to = intersectionVertex(1, 1);

  static Stream<Arguments> noTraverse() {
    return Stream.of(Accessibility.NO_INFORMATION, Accessibility.NOT_POSSIBLE).map(Arguments::of);
  }

  @ParameterizedTest(name = "{0} should be allowed to traverse when requesting onlyAccessible")
  @MethodSource("noTraverse")
  void shouldNotTraverse(Accessibility wheelchair) {
    var req = StreetSearchRequest.of();
    AccessibilityPreferences feature = AccessibilityPreferences.ofOnlyAccessible();
    req
      .withWheelchair(true)
      .withPreferences(preferences ->
        preferences.withWheelchair(
          WheelchairPreferences.of()
            .withTrip(feature)
            .withStop(feature)
            .withElevator(feature)
            .withInaccessibleStreetReluctance(25)
            .withMaxSlope(0.5)
            .withSlopeExceededReluctance(10)
            .withStairsReluctance(25)
            .build()
        )
      );

    var result = traverse(wheelchair, req.build());
    assertTrue(State.isEmpty(result));
  }

  static Stream<Arguments> all() {
    return Stream.of(
      // no extra cost
      Arguments.of(Accessibility.POSSIBLE, 20),
      // low extra cost
      Arguments.of(Accessibility.NO_INFORMATION, 40),
      // high extra cost
      Arguments.of(Accessibility.NOT_POSSIBLE, 3620)
    );
  }

  @ParameterizedTest(name = "{0} should allowed to traverse with a cost of {1}")
  @MethodSource("all")
  void allowByDefault(Accessibility wheelchair, double expectedCost) {
    var req = StreetSearchRequest.of().build();
    var result = traverse(wheelchair, req)[0];
    assertNotNull(result);
    assertTrue(result.weight > 1);

    req = StreetSearchRequest.copyOf(req).withWheelchair(true).build();
    var wheelchairResult = traverse(wheelchair, req)[0];
    assertNotNull(wheelchairResult);
    assertEquals(expectedCost, wheelchairResult.weight);
  }

  @Test
  void testTraversal() {
    var edge = ElevatorHopEdge.createElevatorHopEdge(
      from,
      to,
      StreetTraversalPermission.ALL,
      null,
      2,
      62
    );
    var req = StreetSearchRequest.of().withMode(StreetMode.WALK);
    var res = edge.traverse(new State(from, req.build()))[0];
    assertEquals(62_000, res.getTimeDeltaMilliseconds());
  }

  private State[] traverse(Accessibility wheelchair, StreetSearchRequest req) {
    var edge = ElevatorHopEdge.createElevatorHopEdge(
      from,
      to,
      StreetTraversalPermission.ALL,
      wheelchair
    );
    var state = new State(from, req);

    return edge.traverse(state);
  }
}
