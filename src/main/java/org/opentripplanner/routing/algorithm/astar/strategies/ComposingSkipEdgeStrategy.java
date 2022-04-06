package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Edge;

/**
 * Use several strategies in composition with each other, for example by limiting by time and number
 * of stops visited. Only one needs to be skipped in order for {@link
 * SkipEdgeStrategy#shouldSkipEdge(State, Edge)} to return null.
 */
public record ComposingSkipEdgeStrategy(SkipEdgeStrategy... strategies)
  implements SkipEdgeStrategy {
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
