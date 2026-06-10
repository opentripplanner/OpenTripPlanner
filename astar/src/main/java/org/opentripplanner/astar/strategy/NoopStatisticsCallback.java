package org.opentripplanner.astar.strategy;

import java.util.Set;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.StatisticsCallback;

/**
 * A statistics callback that does nothing.
 */
public class NoopStatisticsCallback<V extends AStarVertex<?, ?, ?>>
  implements StatisticsCallback<V> {

  @Override
  public void searchStarted() {}

  @Override
  public void searchFinished(Set<V> fromVertices, Set<V> toVertices, int verticesVisited) {}
}
