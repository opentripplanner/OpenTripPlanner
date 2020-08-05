package org.opentripplanner.routing.graph_finder;

import org.opentripplanner.model.Stop;

public class StopAndDistance {

  public Stop stop;
  public int distance;

  public StopAndDistance(Stop stop, int distance) {
    this.stop = stop;
    this.distance = distance;
  }
}
