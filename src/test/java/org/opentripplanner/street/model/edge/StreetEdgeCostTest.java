package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.test.support.VariableSource;

class StreetEdgeCostTest extends GraphRoutingTest {

  StreetVertex V1;
  StreetVertex V2;

  public StreetEdgeCostTest() {
    modelOf(
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
  public void walkReluctance(double walkReluctance, long expectedCost) {
    double length = 100;
    var edge = new StreetEdge(V1, V2, null, "edge", length, StreetTraversalPermission.ALL, false);

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withWalk(w -> w.withReluctance(walkReluctance)));
    State result = traverse(edge, req.withMode(StreetMode.WALK).build());
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

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withBike(b -> b.withReluctance(bikeReluctance)));

    State result = traverse(edge, req.withMode(StreetMode.BIKE).build());
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

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withCar(c -> c.withReluctance(carReluctance)));

    State result = traverse(edge, req.withMode(StreetMode.CAR).build());
    assertNotNull(result);
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(9, result.getElapsedTimeSeconds());
  }

  static Stream<Arguments> stairsCases = Stream.of(
    Arguments.of(1, 22),
    Arguments.of(1.5, 33),
    Arguments.of(3, 67)
  );

  @ParameterizedTest(name = "stairs reluctance of {0} should lead to traversal costs of {1}")
  @VariableSource("stairsCases")
  public void stairsReluctance(double stairsReluctance, long expectedCost) {
    double length = 10;
    var edge = new StreetEdge(V1, V2, null, "stairs", length, StreetTraversalPermission.ALL, false);
    edge.setStairs(true);

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withWalk(w -> w.withStairsReluctance(stairsReluctance)));
    req.withMode(StreetMode.WALK);
    var result = traverse(edge, req.build());
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(23, result.getElapsedTimeSeconds());

    edge.setStairs(false);
    var notStairsResult = traverse(edge, req.build());
    assertEquals(15, (long) notStairsResult.weight);
  }

  static Stream<Arguments> bikeStairsCases = Stream.of(
    Arguments.of(1, 45),
    Arguments.of(1.5, 67),
    Arguments.of(3, 135)
  );

  @ParameterizedTest(name = "bike stairs reluctance of {0} should lead to traversal costs of {1}")
  @VariableSource("bikeStairsCases")
  public void bikeStairsReluctance(double stairsReluctance, long expectedCost) {
    double length = 10;
    var edge = new StreetEdge(
      V1,
      V2,
      null,
      "stairs",
      length,
      StreetTraversalPermission.PEDESTRIAN,
      false
    );
    edge.setStairs(true);

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withBike(b -> b.withStairsReluctance(stairsReluctance)));
    req.withMode(StreetMode.BIKE);
    var result = traverse(edge, req.build());
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(23, result.getElapsedTimeSeconds());

    edge.setStairs(false);
    var notStairsResult = traverse(edge, req.build());
    assertEquals(37, (long) notStairsResult.weight);
  }

  static Stream<Arguments> walkSafetyCases = Stream.of(
    Arguments.of(0, 15),
    Arguments.of(0.5, 22),
    Arguments.of(1, 30)
  );

  @ParameterizedTest(name = "walk safety factor of {0} should lead to traversal costs of {1}")
  @VariableSource("walkSafetyCases")
  public void walkSafetyFactor(double walkSafetyFactor, long expectedCost) {
    double length = 10;
    var edge = new StreetEdge(
      V1,
      V2,
      null,
      "test edge",
      length,
      StreetTraversalPermission.ALL,
      false
    );
    edge.setWalkSafetyFactor(2);

    var req = StreetSearchRequest.of();
    req.withPreferences(p -> p.withWalk(w -> w.withSafetyFactor(walkSafetyFactor)));
    req.withMode(StreetMode.WALK);
    var result = traverse(edge, req.build());
    assertEquals(expectedCost, (long) result.weight);

    assertEquals(8, result.getElapsedTimeSeconds());

    edge.setWalkSafetyFactor(1);
    var defaultSafetyResult = traverse(edge, req.build());
    assertEquals(15, (long) defaultSafetyResult.weight);
  }

  private State traverse(StreetEdge edge, StreetSearchRequest request) {
    var state = new State(V1, request);

    assertEquals(0, state.weight);
    return edge.traverse(state);
  }
}
