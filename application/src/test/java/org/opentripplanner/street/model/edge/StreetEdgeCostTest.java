package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.street.model._data.StreetModelForTest.V1;
import static org.opentripplanner.street.model._data.StreetModelForTest.V2;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

class StreetEdgeCostTest {

  static Stream<Arguments> walkReluctanceCases() {
    return Stream.of(
      Arguments.of(0.5, 37),
      Arguments.of(1, 75),
      Arguments.of(2, 150),
      Arguments.of(3, 225)
    );
  }

  @ParameterizedTest(name = "walkRelucance of {0} should lead to traversal costs of {1}")
  @MethodSource("walkReluctanceCases")
  public void walkReluctance(double walkReluctance, long expectedCost) {
    double length = 100;
    var edge = new StreetEdgeBuilder<>()
      .withFromVertex(V1)
      .withToVertex(V2)
      .withName("edge")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withWalk(w -> w.withReluctance(walkReluctance)));
    State result = traverse(edge, req.withMode(StreetMode.WALK).build());
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(76, result.getElapsedTimeSeconds());
  }

  static Stream<Arguments> bikeReluctanceCases() {
    return Stream.of(
      Arguments.of(0.5, 10),
      Arguments.of(1, 20),
      Arguments.of(2, 40),
      Arguments.of(3, 60)
    );
  }

  @ParameterizedTest(name = "bikeReluctance of {0} should lead to traversal costs of {1}")
  @MethodSource("bikeReluctanceCases")
  public void bikeReluctance(double bikeReluctance, long expectedCost) {
    double length = 100;
    var edge = new StreetEdgeBuilder<>()
      .withFromVertex(V1)
      .withToVertex(V2)
      .withName("edge")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withBike(b -> b.withReluctance(bikeReluctance)));

    State result = traverse(edge, req.withMode(StreetMode.BIKE).build());
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(20, result.getElapsedTimeSeconds());
  }

  static Stream<Arguments> bikeSafetyCases() {
    return Stream.of(
      Arguments.of(VehicleRoutingOptimizeType.SHORTEST_DURATION, 20),
      Arguments.of(VehicleRoutingOptimizeType.SAFE_STREETS, 40),
      Arguments.of(VehicleRoutingOptimizeType.SAFEST_STREETS, 160)
    );
  }

  @ParameterizedTest(name = "bikeOptimizeType of {0} should lead to traversal costs of {1}")
  @MethodSource("bikeSafetyCases")
  public void bikeSafety(VehicleRoutingOptimizeType type, long expectedCost) {
    double length = 100;
    var edge = new StreetEdgeBuilder<>()
      .withFromVertex(V1)
      .withToVertex(V2)
      .withName("edge")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .withBicycleSafetyFactor(2)
      .withBack(false)
      .buildAndConnect();

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withBike(b -> b.withReluctance(1.0).withOptimizeType(type)));

    State result = traverse(edge, req.withMode(StreetMode.BIKE).build());
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(20, result.getElapsedTimeSeconds());
  }

  static Stream<Arguments> carReluctanceCases() {
    return Stream.of(
      Arguments.of(0.5, 4),
      Arguments.of(1, 8),
      Arguments.of(2, 17),
      Arguments.of(3, 26)
    );
  }

  @ParameterizedTest(name = "carReluctance of {0} should lead to traversal costs of {1}")
  @MethodSource("carReluctanceCases")
  public void carReluctance(double carReluctance, long expectedCost) {
    double length = 100;
    var edge = new StreetEdgeBuilder<>()
      .withFromVertex(V1)
      .withToVertex(V2)
      .withName("edge")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withCar(c -> c.withReluctance(carReluctance)));

    State result = traverse(edge, req.withMode(StreetMode.CAR).build());
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(9, result.getElapsedTimeSeconds());
  }

  static Stream<Arguments> stairsCases() {
    return Stream.of(Arguments.of(1, 45), Arguments.of(1.5, 67), Arguments.of(3, 135));
  }

  @ParameterizedTest(name = "stairs reluctance of {0} should lead to traversal costs of {1}")
  @MethodSource("stairsCases")
  public void stairsReluctance(double stairsReluctance, long expectedCost) {
    double length = 10;
    var stairsEdge = new StreetEdgeBuilder<>()
      .withFromVertex(V1)
      .withToVertex(V2)
      .withName("stairs")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .withStairs(true)
      .buildAndConnect();

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withWalk(w -> w.withStairsReluctance(stairsReluctance)));
    req.withMode(StreetMode.WALK);
    var result = traverse(stairsEdge, req.build());

    // length / speed * stairsTimeFactor * walkReluctance * stairsReluctance
    assertEquals(expectedCost, (long) result.weight);

    // length / speed * stairsTimeFactor
    assertEquals(23, result.getElapsedTimeSeconds());

    StreetEdge noStairsEdge = stairsEdge.toBuilder().withStairs(false).buildAndConnect();
    var notStairsResult = traverse(noStairsEdge, req.build());
    assertEquals(15, (long) notStairsResult.weight);
  }

  static Stream<Arguments> bikeStairsCases() {
    return Stream.of(Arguments.of(1, 112), Arguments.of(1.5, 169), Arguments.of(3, 338));
  }

  @ParameterizedTest(name = "bike stairs reluctance of {0} should lead to traversal costs of {1}")
  @MethodSource("bikeStairsCases")
  public void bikeStairsReluctance(double stairsReluctance, long expectedCost) {
    double length = 10;
    var stairsEdge = new StreetEdgeBuilder<>()
      .withFromVertex(V1)
      .withToVertex(V2)
      .withName("stairs")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.PEDESTRIAN)
      .withBack(false)
      .withStairs(true)
      .buildAndConnect();

    var req = StreetSearchRequest.of();
    req.withPreferences(p ->
      p.withBike(b -> b.withWalking(w -> w.withStairsReluctance(stairsReluctance)))
    );
    req.withMode(StreetMode.BIKE);
    var result = traverse(stairsEdge, req.build());

    // length / speed * stairsTimeFactor * bikeWalkingReluctance * stairsReluctance
    assertEquals(expectedCost, (long) result.weight);

    // length / speed * stairsTimeFactor
    assertEquals(23, result.getElapsedTimeSeconds());

    StreetEdge noStairsEdge = stairsEdge.toBuilder().withStairs(false).buildAndConnect();
    var notStairsResult = traverse(noStairsEdge, req.build());
    assertEquals(37, (long) notStairsResult.weight);
  }

  static Stream<Arguments> walkSafetyCases() {
    return Stream.of(Arguments.of(0, 15), Arguments.of(0.5, 22), Arguments.of(1, 30));
  }

  @ParameterizedTest(name = "walk safety factor of {0} should lead to traversal costs of {1}")
  @MethodSource("walkSafetyCases")
  public void walkSafetyFactor(double walkSafetyFactor, long expectedCost) {
    double length = 10;
    var safeEdge = new StreetEdgeBuilder<>()
      .withFromVertex(V1)
      .withToVertex(V2)
      .withName("test edge")
      .withMeterLength(length)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .withWalkSafetyFactor(2)
      .buildAndConnect();

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withWalk(w -> w.withSafetyFactor(walkSafetyFactor)));
    req.withMode(StreetMode.WALK);
    var result = traverse(safeEdge, req.build());
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(8, result.getElapsedTimeSeconds());

    StreetEdge lessSafeEdge = safeEdge.toBuilder().withWalkSafetyFactor(1).buildAndConnect();
    var defaultSafetyResult = traverse(lessSafeEdge, req.build());
    assertEquals(15, (long) defaultSafetyResult.weight);
  }

  private State traverse(StreetEdge edge, StreetSearchRequest request) {
    var state = new State(V1, request);

    assertEquals(0, state.weight);
    return edge.traverse(state)[0];
  }
}
