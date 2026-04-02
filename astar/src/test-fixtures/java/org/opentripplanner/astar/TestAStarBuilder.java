package org.opentripplanner.astar;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;

public class TestAStarBuilder
  extends AStarBuilder<TestState, TestEdge, TestVertex, TestAStarBuilder> {

  TestAStarBuilder() {
    super();
    setBuilder(this);
    super.withDominanceFunction((a, b) -> a.getWeight() <= b.getWeight());
  }

  @Override
  protected Duration streetRoutingTimeout() {
    return Duration.ofMinutes(1);
  }

  @Override
  protected Collection<TestState> createInitialStates(Set<TestVertex> originVertices) {
    return originVertices
      .stream()
      .map(v -> new TestState(v, 0))
      .toList();
  }

  @Override
  protected void initializeHeuristic(
    RemainingWeightHeuristic heuristic,
    Set origin,
    Set destination,
    boolean arriveBy
  ) {}

  @Override
  protected DominanceFunction createDefaultDominanceFunction() {
    return null;
  }
}
