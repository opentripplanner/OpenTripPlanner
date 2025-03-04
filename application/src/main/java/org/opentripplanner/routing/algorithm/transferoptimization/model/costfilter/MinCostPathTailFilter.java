package org.opentripplanner.routing.algorithm.transferoptimization.model.costfilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class takes a list of "cost functions" and creates a filter chain for them. The precedence
 * of the cost functions is determined by the order they are listed. For two elements 'a' and 'b'
 * and cost-functions [f1(), f2()] the following is true:
 * <pre>
 *     f1(a) < f1(b) :=  [a]
 *     f1(a) = f1(b) and f2(a) < f2(b) :=  [a]
 *     f1(a) = f1(b) and f2(a) = f2(b) :=  [a, b]
 * </pre>
 *
 * @param <T> The element type of the cost-functions and the filtered list
 */
class MinCostPathTailFilter<T extends RaptorTripSchedule> implements PathTailFilter<T> {

  private final List<ToIntFunction<OptimizedPathTail<T>>> costFunctions;

  MinCostPathTailFilter(List<ToIntFunction<OptimizedPathTail<T>>> costFunctions) {
    this.costFunctions = costFunctions;
  }

  @Override
  public Set<OptimizedPathTail<T>> filterIntermediateResult(
    Set<OptimizedPathTail<T>> elements,
    int boardStopPosition
  ) {
    for (var costFunction : costFunctions) {
      elements = filter(elements, costFunction);
    }
    return elements;
  }

  @Override
  public Set<OptimizedPathTail<T>> filterFinalResult(Set<OptimizedPathTail<T>> elements) {
    return filterIntermediateResult(elements, 0);
  }

  private Set<OptimizedPathTail<T>> filter(
    Set<OptimizedPathTail<T>> elements,
    ToIntFunction<OptimizedPathTail<T>> costFunction
  ) {
    var result = new HashSet<OptimizedPathTail<T>>();
    int minCost = Integer.MAX_VALUE;

    for (var it : elements) {
      int cost = costFunction.applyAsInt(it);

      if (cost == minCost) {
        result.add(it);
      } else if (cost < minCost) {
        minCost = cost;
        result.clear();
        result.add(it);
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(MinCostPathTailFilter.class)
      .addCol("costFunctions", costFunctions)
      .toString();
  }
}
