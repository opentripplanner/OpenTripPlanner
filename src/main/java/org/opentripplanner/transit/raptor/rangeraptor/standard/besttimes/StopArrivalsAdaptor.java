package org.opentripplanner.transit.raptor.rangeraptor.standard.besttimes;

import org.opentripplanner.transit.raptor.api.response.StopArrivals;
import org.opentripplanner.transit.raptor.rangeraptor.standard.internalapi.BestNumberOfTransfers;

public class StopArrivalsAdaptor implements StopArrivals {

  private final BestTimes bestTimes;

  public StopArrivalsAdaptor(BestTimes bestTimes) {
    this.bestTimes = bestTimes;
  }

  @Override
  public boolean reached(int stopIndex) {
    return bestTimes.isStopReached(stopIndex);
  }

  @Override
  public boolean reachedByTransit(int stopIndex) {
    return bestTimes.isStopReachedByTransit(stopIndex);
  }

  @Override
  public int bestTransitArrivalTime(int stopIndex) {
    return bestTimes.transitArrivalTime(stopIndex);
  }
}
