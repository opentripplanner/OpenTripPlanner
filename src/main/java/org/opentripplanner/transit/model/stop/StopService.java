package org.opentripplanner.transit.model.stop;

import java.util.BitSet;
import java.util.Collection;
import org.opentripplanner.raptor.api.path.RaptorStopNameResolver;

public class StopService {

  private final Stop[] stops;
  private final int[] stopBoardAlightCosts;
  private final BitSet[] routingPatternsVisitingStops;

  public StopService(Collection<Stop> stops) {
    this.stops = stops.toArray(new Stop[0]);
    // TODO RTM
    this.stopBoardAlightCosts = new int[0];
    // TODO RTM
    this.routingPatternsVisitingStops = null;
  }

  public int numberOfStops() {
    return stops.length;
  }

  public RaptorStopNameResolver stopNameResolver() {
    return stopIndex -> {
      var s = stops[stopIndex];
      return s == null ? "null" : s.name() + "(" + stopIndex + ")";
    };
  }

  public int[] stopBoardAlightCosts() {
    return stopBoardAlightCosts;
  }

  public BitSet patternMaskForStop(int stopInex) {
    return routingPatternsVisitingStops[stopInex];
  }
}
