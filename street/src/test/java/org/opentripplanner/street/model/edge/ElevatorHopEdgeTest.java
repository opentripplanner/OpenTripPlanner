package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetModelFactory.intersectionVertex;

import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.AccessibilityRequest;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

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
    var feature = AccessibilityRequest.ofOnlyAccessible();
    req
      .withWheelchairEnabled(true)
      .withWheelchair(b ->
        b
          .withStop(feature)
          .withElevator(feature)
          .withInaccessibleStreetReluctance(25)
          .withMaxSlope(0.5)
          .withSlopeExceededReluctance(10)
          .withStairsReluctance(25)
          .build()
      );

    var result = traverse(wheelchair, 1, -1, req.build());
    assertTrue(State.isEmpty(result));
  }

  static Stream<Arguments> all() {
    return Stream.of(
      // no extra cost from accessibility
      Arguments.of(Accessibility.POSSIBLE, 1, -1, Duration.ofSeconds(20), 2.0, true, 40, 20),
      Arguments.of(Accessibility.POSSIBLE, 1, -1, Duration.ofSeconds(20), 2.0, false, 40, 20),
      // low extra cost from accessibility
      Arguments.of(Accessibility.NO_INFORMATION, 1, -1, Duration.ofSeconds(20), 2.0, true, 60, 20),
      Arguments.of(Accessibility.NO_INFORMATION, 1, -1, Duration.ofSeconds(20), 2.0, false, 40, 20),
      // high extra cost from accessibility
      Arguments.of(Accessibility.NOT_POSSIBLE, 1, -1, Duration.ofSeconds(20), 2.0, true, 3640, 20),
      Arguments.of(Accessibility.NOT_POSSIBLE, 1, -1, Duration.ofSeconds(20), 2.0, false, 40, 20),
      // a couple of test cases with different reluctances and a set travel time
      Arguments.of(Accessibility.POSSIBLE, 5, -1, Duration.ofSeconds(30), 1.0, false, 150, 150),
      Arguments.of(Accessibility.POSSIBLE, 5, 25, Duration.ofSeconds(20), 3.0, false, 75, 25)
    );
  }

  @ParameterizedTest(name = "{0} should allowed to traverse with a cost of {1}")
  @MethodSource("all")
  void allowByDefault(
    Accessibility wheelchair,
    double levels,
    int travelTime,
    Duration hopTime,
    double reluctance,
    boolean wheelchairEnabled,
    double expectedCost,
    int expectedDuration
  ) {
    var req = StreetSearchRequest.of()
      .withElevator(elevator -> elevator.withHopTime(hopTime).withReluctance(reluctance))
      .withWheelchairEnabled(wheelchairEnabled)
      .build();
    var result = traverse(wheelchair, levels, travelTime, req)[0];

    assertNotNull(result);
    assertEquals(expectedCost, result.weight);
    assertEquals(expectedDuration, result.getTimeDeltaSeconds());
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

  static Stream<Arguments> outOfServiceCases() {
    return Stream.of(
      Arguments.of(StreetMode.WALK, false),
      Arguments.of(StreetMode.WALK, true),
      Arguments.of(StreetMode.BIKE, false)
    );
  }

  @ParameterizedTest(
    name = "An elevator out of service can not be traversed with {0}, wheelchair {1}"
  )
  @MethodSource("outOfServiceCases")
  void outOfService(StreetMode mode, boolean wheelchair) {
    var edge = ElevatorHopEdge.createElevatorHopEdge(
      from,
      to,
      StreetTraversalPermission.ALL,
      Accessibility.POSSIBLE
    );
    var req = StreetSearchRequest.of().withMode(mode).withWheelchairEnabled(wheelchair).build();

    edge.setOutOfService(true);
    assertTrue(edge.isOutOfService());
    assertTrue(State.isEmpty(edge.traverse(new State(from, req))));

    edge.setOutOfService(false);
    assertFalse(edge.isOutOfService());
    assertFalse(State.isEmpty(edge.traverse(new State(from, req))));
  }

  private State[] traverse(
    Accessibility wheelchair,
    double levels,
    int travelTime,
    StreetSearchRequest req
  ) {
    var edge = ElevatorHopEdge.createElevatorHopEdge(
      from,
      to,
      StreetTraversalPermission.ALL,
      wheelchair,
      levels,
      travelTime
    );
    var state = new State(from, req);

    return edge.traverse(state);
  }
}
