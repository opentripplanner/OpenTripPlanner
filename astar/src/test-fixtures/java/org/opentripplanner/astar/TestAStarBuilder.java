package org.opentripplanner.astar;

import java.time.Duration;
import java.util.List;
import java.util.Set;

public class TestAStarBuilder {

  public static AStarBuilder<TestState, TestEdge, TestVertex> ofDefault(
    TestVertex origin,
    TestVertex destination
  ) {
    return new AStarBuilder<TestState, TestEdge, TestVertex>()
      .withGoalVertices(Set.of(destination))
      .withTimeout(Duration.ofMinutes(5))
      .withDominanceFunction((a, b) -> a.getWeight() <= b.getWeight())
      .withInitialStates(List.of(createInitialState(origin)));
  }

  private static TestState createInitialState(TestVertex vertex) {
    return new TestState(vertex, 0);
  }
}
