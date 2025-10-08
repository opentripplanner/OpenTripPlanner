package org.opentripplanner.astar.strategy;

import java.util.Comparator;
import org.opentripplanner.astar.model.GraphPath;

public class DurationComparator implements Comparator<GraphPath<?, ?, ?>> {

  @Override
  public int compare(GraphPath o1, GraphPath o2) {
    return o1.getDuration() - o2.getDuration();
  }
}
