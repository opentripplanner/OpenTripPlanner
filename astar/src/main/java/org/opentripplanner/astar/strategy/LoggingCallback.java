package org.opentripplanner.astar.strategy;

import java.util.Set;
import org.opentripplanner.astar.spi.AStarVertex;
import org.opentripplanner.astar.spi.StatisticsCallback;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingCallback<V extends AStarVertex<?, ?, ?>> implements StatisticsCallback<V> {

  private static final Logger LOG = LoggerFactory.getLogger(LoggingCallback.class);

  private double start_Ms;

  @Override
  public void searchStarted() {
    start_Ms = System.currentTimeMillis();
  }

  @Override
  public void searchFinished(Set<V> fromVertices, Set<V> toVertices, int verticesVisited) {
    long millis = (long) (System.currentTimeMillis() - start_Ms);
    var duration = DurationUtils.durationToStrMillisescond(millis);
    LOG.info(
      "Statistics for A* search: fromVertices={}, toVertices={}, verticesVisited={}, duration={}",
      fromVertices,
      toVertices,
      verticesVisited,
      duration
    );
  }
}
