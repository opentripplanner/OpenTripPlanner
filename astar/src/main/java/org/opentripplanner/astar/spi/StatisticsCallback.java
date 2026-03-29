package org.opentripplanner.astar.spi;

import java.util.Set;
import org.opentripplanner.astar.strategy.NoopStatisticsCallback;

/**
 * A callback for receiving statistics about A* searches.
 */
public interface StatisticsCallback<V extends AStarVertex<?, ?, ?>> {
  StatisticsCallback NOOP = new NoopStatisticsCallback<>();

  void searchStarted();

  void searchFinished(Set<V> fromVertices, Set<V> toVertices, int verticesVisited);
}
