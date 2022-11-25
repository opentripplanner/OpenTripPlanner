package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostFilterChain;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;

public class TransferOptimizedFilterFactory<T extends RaptorTripSchedule> {

  public static <T extends RaptorTripSchedule> MinCostFilterChain<OptimizedPathTail<T>> filter(
    boolean transferPriority,
    boolean optimizeWaitTime
  ) {
    return new TransferOptimizedFilterFactory<T>().create(transferPriority, optimizeWaitTime);
  }

  private MinCostFilterChain<OptimizedPathTail<T>> create(
    boolean transferPriority,
    boolean optimizeWaitTime
  ) {
    List<ToIntFunction<OptimizedPathTail<T>>> filters = new ArrayList<>(3);

    if (transferPriority) {
      filters.add(OptimizedPathTail::transferPriorityCost);
    }

    if (optimizeWaitTime) {
      filters.add(OptimizedPathTail::generalizedCostWaitTimeOptimized);
    } else {
      filters.add(OptimizedPathTail::generalizedCost);
    }

    filters.add(OptimizedPathTail::breakTieCost);

    return new MinCostFilterChain<>(filters);
  }
}
