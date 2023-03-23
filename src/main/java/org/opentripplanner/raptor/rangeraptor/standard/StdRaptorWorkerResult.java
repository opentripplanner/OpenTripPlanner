package org.opentripplanner.raptor.rangeraptor.standard;

import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.BestTimes;
import org.opentripplanner.raptor.rangeraptor.standard.internalapi.StopArrivalsState;

/**
 * Result for Standard Range Raptor route call.
 */
public class StdRaptorWorkerResult<T extends RaptorTripSchedule> implements RaptorWorkerResult<T> {

  private final BestTimes bestTimes;
  private final StopArrivalsState<T> state;

  public StdRaptorWorkerResult(BestTimes bestTimes, StopArrivalsState<T> state) {
    this.bestTimes = bestTimes;
    this.state = state;
  }

  @Override
  public Collection<RaptorPath<T>> extractPaths() {
    return state.extractPaths();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestOverallArrivals() {
    return bestTimes.extractBestOverallArrivals();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestTransitArrivals() {
    return bestTimes.extractBestTransitArrivals();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestNumberOfTransfers() {
    return state.extractBestNumberOfTransfers();
  }
}
