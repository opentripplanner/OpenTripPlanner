package org.opentripplanner.routing.impl;

import java.util.Comparator;
import org.opentripplanner.routing.spt.GraphPath;

public class DurationComparator implements Comparator<GraphPath> {

  @Override
  public int compare(GraphPath o1, GraphPath o2) {
    return o1.getDuration() - o2.getDuration();
  }
}
