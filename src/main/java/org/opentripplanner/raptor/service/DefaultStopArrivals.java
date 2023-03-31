package org.opentripplanner.raptor.service;

import org.opentripplanner.raptor.api.response.StopArrivals;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;

/**
 * Cache results and extract information lazy when accessed.
 */
public class DefaultStopArrivals implements StopArrivals {

  private SingleCriteriaStopArrivals bestOverallArrivalTime = null;
  private SingleCriteriaStopArrivals bestTransitArrivalTime = null;
  private SingleCriteriaStopArrivals bestNumberOfTransfers = null;

  private final RaptorWorkerResult<?> results;

  public DefaultStopArrivals(RaptorWorkerResult<?> results) {
    this.results = results;
  }

  @Override
  public boolean reached(int stopIndex) {
    return bestOverallArrivalTime().isReached(stopIndex);
  }

  @Override
  public int bestArrivalTime(int stopIndex) {
    return bestOverallArrivalTime().value(stopIndex);
  }

  @Override
  public boolean reachedByTransit(int stopIndex) {
    return bestTransitArrivalTime().isReached(stopIndex);
  }

  @Override
  public int bestTransitArrivalTime(int stopIndex) {
    return bestTransitArrivalTime().value(stopIndex);
  }

  private SingleCriteriaStopArrivals bestOverallArrivalTime() {
    if (bestOverallArrivalTime == null) {
      this.bestOverallArrivalTime = results.extractBestOverallArrivals();
    }
    return bestOverallArrivalTime;
  }

  private SingleCriteriaStopArrivals bestTransitArrivalTime() {
    if (bestTransitArrivalTime == null) {
      this.bestTransitArrivalTime = results.extractBestTransitArrivals();
    }
    return bestTransitArrivalTime;
  }

  private SingleCriteriaStopArrivals bestNumberOfTransfers() {
    if (bestNumberOfTransfers == null) {
      this.bestNumberOfTransfers = results.extractBestNumberOfTransfers();
    }
    return bestNumberOfTransfers;
  }
}
