package org.opentripplanner.astar.strategy;

import java.util.HashSet;
import org.opentripplanner.astar.spi.AStarEdge;
import org.opentripplanner.astar.spi.AStarState;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.street.model.vertex.TransitStopVertex;

/**
 * Skips edges when the specified number of stops have been visited
 */
public class MaxCountSkipEdgeStrategy<
  State extends AStarState<State, Edge, ?>, Edge extends AStarEdge<State, Edge, ?>
>
  implements SkipEdgeStrategy<State, Edge> {

  private final int maxCount;
  private final HashSet<TransitStopVertex> stopsCounted = new HashSet<>();

  public MaxCountSkipEdgeStrategy(int count) {
    this.maxCount = count;
  }

  @Override
  public boolean shouldSkipEdge(State current, Edge edge) {
    if (current.getVertex() instanceof TransitStopVertex stopVertex) {
      stopsCounted.add(stopVertex);
    }
    return stopsCounted.size() > maxCount;
  }
}
