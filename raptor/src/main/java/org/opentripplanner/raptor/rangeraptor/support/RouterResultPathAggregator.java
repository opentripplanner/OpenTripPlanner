package org.opentripplanner.raptor.rangeraptor.support;

import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.rangeraptor.internalapi.RaptorRouterResult;
import org.opentripplanner.raptor.rangeraptor.internalapi.SingleCriteriaStopArrivals;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * Aggregates the results of multiple Raptor searches (a multi-segment search).
 * <p>
 * The methods {@link #extractPaths()} and {@link #isDestinationReached()} return fully aggregated
 * results based on all segments.
 * <p>
 * The other methods that report per-stop statistics are <b>not</b> fully aggregated across
 * segments. Instead, they return the statistics from the first segment only. These statistics
 * are used for analysis and debugging, and are relatively expensive to compute. Fully aggregating
 * them across all segments could introduce unnecessary overhead and risk if these methods were
 * ever called as part of a normal transit search.
 */
public class RouterResultPathAggregator<T extends RaptorTripSchedule>
  implements RaptorRouterResult<T> {

  // Keep the result of one of the segments, and provide it for the one-to-many
  // statistics results methods. See JavaDoc on class.
  private final RaptorRouterResult<T> master;
  private final ParetoSet<RaptorPath<T>> paths;

  public RouterResultPathAggregator(
    Collection<RaptorRouterResult<T>> results,
    ParetoComparator<RaptorPath<T>> comparator
  ) {
    this.paths = ParetoSet.of(comparator);
    RaptorRouterResult<T> first = null;
    for (var it : results) {
      if (first == null) {
        first = it;
      }
      this.paths.addAll(it.extractPaths());
    }
    this.master = first;
  }

  @Override
  public Collection<RaptorPath<T>> extractPaths() {
    return paths.stream().toList();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestOverallArrivals() {
    return master.extractBestOverallArrivals();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestTransitArrivals() {
    return master.extractBestTransitArrivals();
  }

  @Override
  public SingleCriteriaStopArrivals extractBestNumberOfTransfers() {
    return master.extractBestNumberOfTransfers();
  }

  @Override
  public boolean isDestinationReached() {
    return !paths.isEmpty();
  }
}
