package org.opentripplanner.astar.strategy;

import java.util.function.Function;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;

/**
 * Skips edges when the specified number of desired vertices have been visited
 */
public class MaxCountSkipEdgeStrategy<
  State extends AStarState<State, Edge, ?>, Edge extends AStarEdge<State, Edge, ?>
>
  implements SkipEdgeStrategy<State, Edge> {

  private final int maxCount;
  private final Function<State, Boolean> shouldIncreaseCount;

  private int visited;

  public MaxCountSkipEdgeStrategy(int count, Function<State, Boolean> shouldIncreaseCount) {
    this.maxCount = count;
    this.shouldIncreaseCount = shouldIncreaseCount;
    this.visited = 0;
  }

  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    if (this.shouldIncreaseCount.apply(current)) {
      visited++;
    }
    return visited > maxCount;
  }
}
