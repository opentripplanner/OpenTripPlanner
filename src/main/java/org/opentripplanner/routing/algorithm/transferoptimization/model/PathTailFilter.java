package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.Set;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * TODO PT - This need JavaDoc
 */
public interface PathTailFilter<T extends RaptorTripSchedule> {
  /**
   * TODO PT - This need JavaDoc
   */
  Set<OptimizedPathTail<T>> filterIntermediateResult(Set<OptimizedPathTail<T>> elements);

  /**
   * TODO PT - This need JavaDoc
   */
  Set<OptimizedPathTail<T>> filterFinalResult(Set<OptimizedPathTail<T>> elements);
}
