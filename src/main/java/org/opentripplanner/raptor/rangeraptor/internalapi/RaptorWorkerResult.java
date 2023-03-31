package org.opentripplanner.raptor.rangeraptor.internalapi;

import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;

/**
 * This is the result of the {@link RaptorWorker#route()} call.
 */
public interface RaptorWorkerResult<T extends RaptorTripSchedule> {
  /**
   * Return all paths found.
   */
  Collection<RaptorPath<T>> extractPaths();

  /**
   * Get "best-overall" arrival statistics for each stop reached in the search.
   */
  SingleCriteriaStopArrivals extractBestOverallArrivals();

  /**
   * Get transit arrival statistics for each stop reached in the search.
   */
  SingleCriteriaStopArrivals extractBestTransitArrivals();

  /**
   * Extract information about the best number of transfers for each stop arrival.
   */
  SingleCriteriaStopArrivals extractBestNumberOfTransfers();

  /**
   * Return {@code true} if the destination was reached at least once.
   */
  boolean isDestinationReached();
}
