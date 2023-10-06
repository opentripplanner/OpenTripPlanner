package org.opentripplanner.routing.algorithm.transferoptimization.model.costfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilterFactory;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

public class MinCostPathTailFilterFactory<T extends RaptorTripSchedule>
  implements PathTailFilterFactory<T> {

  private final boolean transferPriority;
  private final boolean optimizeWaitTime;

  public MinCostPathTailFilterFactory(boolean transferPriority, boolean optimizeWaitTime) {
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

  /**
   * This factory method is used for unit testing. It allows you to pass in a simple cost function
   * instead of the more complicated functions used in the main version of this.
   */
  public static <T extends RaptorTripSchedule> PathTailFilter<OptimizedPathTail<T>> ofCostFunction(
    ToIntFunction<OptimizedPathTail<T>> costFunction
  ) {
    return new MinCostPathTailFilter<>(List.of(costFunction));
  }
}
