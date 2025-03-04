package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.transit.model.site.PathwayMode;

class PathwayEdgeTest {

  Vertex from = intersectionVertex(10, 10);
  Vertex to = intersectionVertex(10.001, 10.001);

  @Test
  void zeroLength() {
    // if elevators have a traversal time and distance of 0 we cannot interpolate the distance
    // from the vertices as they most likely have identical coordinates
    var edge = PathwayEdge.createPathwayEdge(
      from,
      to,
      new NonLocalizedString("pathway"),
      0,
      0,
      0,
      0,
      true,
      PathwayMode.ELEVATOR
    );

    assertThatEdgeIsTraversable(edge);
  }

  @Test
  void zeroLengthWithSteps() {
    var edge = PathwayEdge.createPathwayEdge(
      from,
      to,
      new NonLocalizedString("pathway"),
      0,
      0,
      2,
      0,
      true,
      PathwayMode.STAIRS
    );

    assertThatEdgeIsTraversable(edge);
  }

  @Test
  void traversalTime() {
    var edge = PathwayEdge.createPathwayEdge(
      from,
      to,
      new NonLocalizedString("pathway"),
      60,
      0,
      0,
      0,
      true,
      PathwayMode.ESCALATOR
    );

    var state = assertThatEdgeIsTraversable(edge);
    assertEquals(60, state.getElapsedTimeSeconds());
    assertEquals(120, state.getWeight());
  }

  @Test
  void traversalTimeOverridesLength() {
    var edge = PathwayEdge.createPathwayEdge(
      from,
      to,
      new NonLocalizedString("pathway"),
      60,
      1000,
      0,
      0,
      true,
      PathwayMode.MOVING_SIDEWALK
    );

    assertEquals(1000, edge.getDistanceMeters());

    var state = assertThatEdgeIsTraversable(edge);
    assertEquals(60, state.getElapsedTimeSeconds());
    assertEquals(120, state.getWeight());
  }

  @Test
  void distance() {
    var edge = PathwayEdge.createPathwayEdge(
      from,
      to,
      new NonLocalizedString("pathway"),
      0,
      60,
      0,
      0,
      true,
      PathwayMode.WALKWAY
    );

    var state = assertThatEdgeIsTraversable(edge);
    assertEquals(6, state.getElapsedTimeSeconds());
    assertEquals(12, state.getWeight());
  }

  @Test
  void wheelchair() {
    var edge = PathwayEdge.createPathwayEdge(
      from,
      to,
      new NonLocalizedString("pathway"),
      0,
      60,
      0,
      0,
      false,
      PathwayMode.WALKWAY
    );

    var state = assertThatEdgeIsTraversable(edge, true);
    assertEquals(6, state.getElapsedTimeSeconds());
    assertEquals(300.0, state.getWeight());
  }

  static Stream<Arguments> slopeCases() {
    return Stream.of(
      // no extra cost
      Arguments.of(0.07, 120),
      // no extra cost
      Arguments.of(0.08, 120),
      // 1 % above max
      Arguments.of(0.09, 239),
      // 1.1 % above the max slope, tiny extra cost
      Arguments.of(0.091, 251),
      // 1.15 % above the max slope, will incur larger cost
      Arguments.of(0.0915, 257),
      // 3 % above max slope, will incur very large cost
      Arguments.of(0.11, 480)
    );
  }

  /**
   * This makes sure that when you exceed the max slope in a wheelchair there isn't a hard cut-off
   * but rather the cost increases proportional to how much you go over the maximum.
   * <p>
   * In other words: 0.1 % over the limit only has a small cost but 2% over increases it
   * dramatically to the point where it's only used as a last resort.
   */
  @ParameterizedTest(name = "slope of {0} should lead to traversal costs of {1}")
  @MethodSource("slopeCases")
  void shouldScaleCostWithMaxSlope(double slope, long expectedCost) {
    var edge = PathwayEdge.createPathwayEdge(
      from,
      to,
      new NonLocalizedString("pathway"),
      60,
      100,
      0,
      slope,
      true,
      PathwayMode.WALKWAY
    );

    var state = assertThatEdgeIsTraversable(edge, true);
    assertEquals(60, state.getElapsedTimeSeconds());
    assertEquals(expectedCost, (int) state.getWeight());
  }

  private State assertThatEdgeIsTraversable(PathwayEdge edge) {
    return assertThatEdgeIsTraversable(edge, false);
  }

  private State assertThatEdgeIsTraversable(PathwayEdge edge, boolean wheelchair) {
    var req = StreetSearchRequest.of().withWheelchair(wheelchair).withMode(StreetMode.WALK);

    req.withPreferences(preferences ->
      preferences
        .withWalk(builder -> builder.withSpeed(10))
        .withWheelchair(
          WheelchairPreferences.of()
            .withTripOnlyAccessible()
            .withStopOnlyAccessible()
            .withElevatorOnlyAccessible()
            .withInaccessibleStreetReluctance(25)
            .withMaxSlope(0.08)
            .withSlopeExceededReluctance(1)
            .withStairsReluctance(25)
            .build()
        )
    );

    var afterTraversal = edge.traverse(new State(from, req.build()))[0];
    assertNotNull(afterTraversal);

    assertTrue(afterTraversal.getWeight() > 0);
    return afterTraversal;
  }

  @Nested
  class SignpostedAs {

    @Test
    void signpostedAs() {
      var sign = I18NString.of("sign");
      var edge = pathwayEdge(sign);
      assertEquals(Optional.of(sign), edge.signpostedAs());
      assertEquals(sign, edge.getName());
    }

    @Test
    void nullSignpostedAs() {
      var edge = pathwayEdge(null);
      assertEquals(Optional.empty(), edge.signpostedAs());
      assertEquals(PathwayEdge.DEFAULT_NAME, edge.getName());
    }

    @Test
    void emptySignpostedAs() {
      var edge = PathwayEdge.createLowCostPathwayEdge(from, to, PathwayMode.WALKWAY);
      assertEquals(Optional.empty(), edge.signpostedAs());
      assertEquals(PathwayEdge.DEFAULT_NAME, edge.getName());
    }

    private PathwayEdge pathwayEdge(I18NString sign) {
      return PathwayEdge.createPathwayEdge(
        from,
        to,
        sign,
        60,
        100,
        0,
        0,
        false,
        PathwayMode.WALKWAY
      );
    }
  }
}
