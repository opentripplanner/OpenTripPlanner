package org.opentripplanner.astar.strategy;

import java.util.function.Predicate;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;

/**
 * Skips edges when the specified number of desired vertices have been visited.
 */
public class MaxCountSkipEdgeStrategy<
  State extends AStarState<State, Edge, ?>, Edge extends AStarEdge<State, Edge, ?>
>
  implements SkipEdgeStrategy<State, Edge> {

  private final int maxCount;
  private final Predicate<State> shouldIncreaseCount;

  private int visited;

  public MaxCountSkipEdgeStrategy(int count, Predicate<State> shouldIncreaseCount) {
    this.maxCount = count;
    this.shouldIncreaseCount = shouldIncreaseCount;
    this.visited = 0;
  }

  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    if (shouldIncreaseCount.test(current)) {
      visited++;
    }
    return visited > maxCount;
  }
}
