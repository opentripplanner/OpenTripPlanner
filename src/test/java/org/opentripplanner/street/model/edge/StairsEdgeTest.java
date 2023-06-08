package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model._data.StreetModelForTest.V1;
import static org.opentripplanner.street.model._data.StreetModelForTest.V2;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.test.support.VariableSource;

class StairsEdgeTest {

  private static final double LENGTH = 10;
  private static final StairsEdge STAIRS_EDGE = new StairsEdge(
    V1,
    V2,
    GeometryUtils.makeLineString(V1.getCoordinate(), V2.getCoordinate()),
    new NonLocalizedString("stairs"),
    LENGTH
  );

  static Stream<Arguments> stairsCases = Stream.of(
    Arguments.of(1, 22),
    Arguments.of(1.5, 33),
    Arguments.of(3, 67)
  );

  @ParameterizedTest(name = "stairs reluctance of {0} should lead to traversal costs of {1}")
  @VariableSource("stairsCases")
  public void stairsReluctance(double stairsReluctance, long expectedCost) {
    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withWalk(w -> w.withStairsReluctance(stairsReluctance)));
    req.withMode(StreetMode.WALK);
    var result = traverse(STAIRS_EDGE, req.build());
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(22, result.getElapsedTimeSeconds());

    var streetEdge = new StreetEdge(
      V1,
      V2,
      null,
      "stairs",
      LENGTH,
      StreetTraversalPermission.ALL,
      false
    );
    var notStairsResult = traverse(streetEdge, req.build());
    assertEquals(15, (long) notStairsResult.weight);
  }

  static Stream<Arguments> bikeStairsCases = Stream.of(
    Arguments.of(1, 45),
    Arguments.of(1.5, 68),
    Arguments.of(3, 135)
  );

  @ParameterizedTest(name = "bike stairs reluctance of {0} should lead to traversal costs of {1}")
  @VariableSource("bikeStairsCases")
  public void bikeStairsReluctance(double stairsReluctance, double expectedCost) {
    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withBike(b -> b.withStairsReluctance(stairsReluctance)));
    req.withMode(StreetMode.BIKE);
    var result = traverse(STAIRS_EDGE, req.build());
    assertEquals(expectedCost, result.weight, 0.5);

    assertEquals(22, result.getElapsedTimeSeconds());
    var streetEdge = new StreetEdge(V1, V2, null, "stairs", LENGTH, PEDESTRIAN, false);

    var notStairsResult = traverse(streetEdge, req.build());
    assertEquals(37, (long) notStairsResult.weight);
  }

  static Stream<Arguments> wheelchairStairsCases = Stream.of(
    Arguments.of(1, 22),
    Arguments.of(10, 225),
    Arguments.of(100, 2255)
  );

  @ParameterizedTest(
    name = "wheelchair stairs reluctance of {0} should lead to traversal costs of {1}"
  )
  @VariableSource("wheelchairStairsCases")
  public void wheelchairStairsReluctance(double stairsReluctance, long expectedCost) {
    var req = StreetSearchRequest.of();
    req.withWheelchair(true);
    req.withPreferences(preferences ->
      preferences.withWheelchair(
        WheelchairPreferences
          .of()
          .withTripOnlyAccessible()
          .withStopOnlyAccessible()
          .withElevatorOnlyAccessible()
          .withInaccessibleStreetReluctance(25)
          .withMaxSlope(0)
          .withSlopeExceededReluctance(1.1)
          .withStairsReluctance(stairsReluctance)
          .build()
      )
    );

    req.withPreferences(pref -> pref.withWalk(w -> w.withReluctance(1.0)));

    var result = traverse(STAIRS_EDGE, req.build());
    assertEquals(expectedCost, (long) result.weight);

    var edge = new StreetEdge(V1, V2, null, "stairs", LENGTH, StreetTraversalPermission.ALL, false);
    var notStairsResult = traverse(edge, req.build());
    assertEquals(7, (long) notStairsResult.weight);
  }

  @Test
  void noCar() {
    var req = StreetSearchRequest.of();
    req.withMode(StreetMode.CAR);
    var state = new State(V1, req.build());
    var result = STAIRS_EDGE.traverse(state);
    assertTrue(State.isEmpty(result));
  }

  static Stream<Arguments> barrierCases = Stream.of(
    Arguments.of(PEDESTRIAN, false),
    Arguments.of(NONE, true)
  );

  @ParameterizedTest(name = "a barrier permission of {0} should block walking traversal={1}")
  @VariableSource("barrierCases")
  void barriers(StreetTraversalPermission permission, boolean shouldBlockTraversal) {
    var barrier = StreetModelForTest.barrierVertex(V2.getLat(), V2.getLon());
    barrier.setBarrierPermissions(permission);

    var edge = new StairsEdge(
      V1,
      barrier,
      GeometryUtils.makeLineString(V1.getCoordinate(), V2.getCoordinate()),
      new NonLocalizedString("stairs"),
      LENGTH
    );

    var req = StreetSearchRequest.of();
    req.withMode(StreetMode.WALK);
    var state = new State(V1, req.build());
    var result = edge.traverse(state);
    assertEquals(shouldBlockTraversal, State.isEmpty(result));
  }

  private State traverse(OsmEdge edge, StreetSearchRequest request) {
    var state = new State(V1, request);
    assertEquals(0, state.weight);
    return edge.traverse(state)[0];
  }
}
