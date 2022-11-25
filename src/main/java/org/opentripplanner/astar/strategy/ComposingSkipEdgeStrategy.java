package org.opentripplanner.astar.strategy;

import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;

/**
 * Use several strategies in composition with each other, for example by limiting by time and number
 * of stops visited. Only one needs to be skipped in order for {@link
 * SkipEdgeStrategy#shouldSkipEdge(State, Edge)} to return null.
 */
public record ComposingSkipEdgeStrategy<
  State extends AStarState<State, Edge, ?>, Edge extends AStarEdge<State, Edge, ?>
>(SkipEdgeStrategy<State, Edge>... strategies)
  implements SkipEdgeStrategy<State, Edge> {
  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    for (var strategy : strategies) {
      if (strategy.shouldSkipEdge(current, edge)) {
        return true;
      }
    }
    return false;
  }
}
