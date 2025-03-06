package org.opentripplanner.raptor.rangeraptor;

import java.util.Collection;
import java.util.function.BiFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;

/**
 * Join two results together.
 * <ul>
 *   <li>Everything from the first result is added</li>
 *   <li>The result is merged with the injected merge strategy.</li>
 *   <li>Some of the methods ONLY return the result of the main search!</li>
 * </ul>
 */
class CompositeResult<T extends RaptorTripSchedule> implements RaptorRouterResult<T> {

  private static final String UNSUPPORTED_OPERATION =
    "Merging all stop arrivals will be a complicated and memory intensive process, unless we need this this should not be done.";
  private final Collection<RaptorPath<T>> result;

  CompositeResult(
    RaptorRouterResult<T> mainResult,
    RaptorRouterResult<T> alternativeResult,
    BiFunction<
      Collection<RaptorPath<T>>,
      Collection<RaptorPath<T>>,
      Collection<RaptorPath<T>>
    > merger
  ) {
    this.result = merger.apply(mainResult.extractPaths(), alternativeResult.extractPaths());
  }

  /**
   * Return the merged result.
   */
  @Override
  public Collection<RaptorPath<T>> extractPaths() {
    return result;
  }

  @Override
  public SingleCriteriaStopArrivals extractBestOverallArrivals() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
  }

  @Override
  public SingleCriteriaStopArrivals extractBestTransitArrivals() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
  }

  @Override
  public SingleCriteriaStopArrivals extractBestNumberOfTransfers() {
    throw new UnsupportedOperationException(UNSUPPORTED_OPERATION);
  }

  /**
   * Return true if either the main or the alternative search has reached the destination.
   */
  @Override
  public boolean isDestinationReached() {
    return !result.isEmpty();
  }
}
