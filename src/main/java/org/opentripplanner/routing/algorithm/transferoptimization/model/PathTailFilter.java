package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Set;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Filter path tails for a given stopPosition during the optimization process. The algorithm only
 * feeds in paths which can be compared. If the head is alighted at a position which gives it an
 * advantage over another path (accept boarding at more stops), then the paths are split into
 * different sets and the filter is called for each set.
 */
public interface PathTailFilter<T extends RaptorTripSchedule> {
  /**
   * Filter path while building the paths. The {@code head} of each path is guaranteed to be a
   * transit leg, and the {@code boardStopPosition} is guaranteed to be the last position in the
   * head leg which will be used for boarding. The {@code boardStopPosition} should be used when
   * calculating the property which is used for comparison. If the comparison can be done without
   * looking at a stop-position, then this can be ignored.
   */
  Set<OptimizedPathTail<T>> filterIntermediateResult(
    Set<OptimizedPathTail<T>> elements,
    int boardStopPosition
  );

  /**
   * Filter the paths one last time. The {@code head} is not guaranteed to be the access-leg. This
   * can be used to insert values into the path or checking if all requirements are meet.
   *
   */
  Set<OptimizedPathTail<T>> filterFinalResult(Set<OptimizedPathTail<T>> elements);
}
