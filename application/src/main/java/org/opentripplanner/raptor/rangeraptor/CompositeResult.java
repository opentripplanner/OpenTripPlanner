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

  private final RaptorRouterResult<T> mainResult;
  private final Collection<RaptorPath<T>> result;

  CompositeResult(
    RaptorRouterResult<T> mainResult,
    RaptorRouterResult<T> alternativeResult,
    BiFunction<Collection<RaptorPath<T>>, Collection<RaptorPath<T>>, Collection<RaptorPath<T>>> merger
  ) {
    this.mainResult = mainResult;
    this.result = merger.apply(mainResult.extractPaths(), alternativeResult.extractPaths());
  }

  /**
   * Return the merged result.
   */
  @Override
  public Collection<RaptorPath<T>> extractPaths() {
    return result;
  }

  /**
   * Return the <b>main</b> result only.
   */
  @Override
  public SingleCriteriaStopArrivals extractBestOverallArrivals() {
    return mainResult.extractBestOverallArrivals();
  }

  /**
   * Return the <b>main</b> result only.
   */
  @Override
  public SingleCriteriaStopArrivals extractBestTransitArrivals() {
    return mainResult.extractBestTransitArrivals();
  }

  /**
   * Return the <b>main</b> result only.
   */
  @Override
  public SingleCriteriaStopArrivals extractBestNumberOfTransfers() {
    return mainResult.extractBestNumberOfTransfers();
  }

  /**
   * Return true if either the main or the alternative search has reached the destination.
   */
  @Override
  public boolean isDestinationReached() {
    return !result.isEmpty();
  }
}
