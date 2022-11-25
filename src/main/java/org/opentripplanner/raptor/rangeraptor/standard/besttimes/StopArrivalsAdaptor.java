package org.opentripplanner.raptor.rangeraptor.standard.besttimes;

import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.BestNumberOfTransfers;

public class StopArrivalsAdaptor implements StopArrivals {

  private final BestTimes bestTimes;
  private final BestNumberOfTransfers nTransfers;

  public StopArrivalsAdaptor(BestTimes bestTimes, BestNumberOfTransfers nTransfers) {
    this.bestTimes = bestTimes;
    this.nTransfers = nTransfers;
  }

  @Override
  public boolean reached(int stopIndex) {
    return bestTimes.isStopReached(stopIndex);
  }

  @Override
  public int bestArrivalTime(int stopIndex) {
    return bestTimes.time(stopIndex);
  }

  @Override
  public boolean reachedByTransit(int stopIndex) {
    return bestTimes.isStopReachedByTransit(stopIndex);
  }

  @Override
  public int bestTransitArrivalTime(int stopIndex) {
    return bestTimes.transitArrivalTime(stopIndex);
  }

  @Override
  public int smallestNumberOfTransfers(int stopIndex) {
    return nTransfers.calculateMinNumberOfTransfers(stopIndex);
  }
}
