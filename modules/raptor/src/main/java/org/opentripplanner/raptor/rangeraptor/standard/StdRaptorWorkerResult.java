package org.opentripplanner.raptor.rangeraptor.standard;

import java.util.Collection;
import java.util.function.Supplier;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorWorkerResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;
import org.opentripplanner.raptor.rangeraptor.standard.besttimes.BestTimes;

/**
 * Result for Standard Range Raptor route call.
 */
public class StdRaptorWorkerResult<T extends RaptorTripSchedule> implements RaptorWorkerResult<T> {

  private final BestTimes bestTimes;
  private final Supplier<Collection<RaptorPath<T>>> pathSupplier;
  private final Supplier<SingleCriteriaStopArrivals> bestNumberOfTransfersSupplier;

  /**
   * Cash paths to avoid constructing hundreds of paths several times. In most cases this is
   * not a problem, but in rare cases there are a lot of paths.
   */
  private Collection<RaptorPath<T>> paths = null;

  public StdRaptorWorkerResult(
    BestTimes bestTimes,
    Supplier<Collection<RaptorPath<T>>> pathSupplier,
    Supplier<SingleCriteriaStopArrivals> bestNumberOfTransfersSupplier
  ) {
    this.bestTimes = bestTimes;
    this.pathSupplier = pathSupplier;
    this.bestNumberOfTransfersSupplier = bestNumberOfTransfersSupplier;
  }

  @Override
  public Collection<RaptorPath<T>> extractPaths() {
    if (paths == null) {
      paths = pathSupplier.get();
    }
    return paths;
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
    return bestNumberOfTransfersSupplier.get();
  }

  @Override
  public boolean isDestinationReached() {
    return !extractPaths().isEmpty();
  }
}
