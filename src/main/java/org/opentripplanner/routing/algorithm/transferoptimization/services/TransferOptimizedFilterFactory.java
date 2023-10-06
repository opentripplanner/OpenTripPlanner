package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.MinCostPathTailFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilterFactory;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

public class TransferOptimizedFilterFactory<T extends RaptorTripSchedule>
  implements PathTailFilterFactory<T> {

  private final boolean transferPriority;
  private final boolean optimizeWaitTime;

  public TransferOptimizedFilterFactory(boolean transferPriority, boolean optimizeWaitTime) {
    this.transferPriority = transferPriority;
    this.optimizeWaitTime = optimizeWaitTime;
  }

  @Override
  public PathTailFilter<OptimizedPathTail<T>> createFilter(
    List<List<TripToTripTransfer<T>>> possibleTransfers
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

    return new MinCostPathTailFilter<>(filters);
  }
}
