package org.opentripplanner.routing.algorithm.transferoptimization.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToIntFunction;

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
public class MinCostFilterChain<T> implements OptimizeTransfersFilterChain<T> {

  private final List<ToIntFunction<T>> costFunctions;

  public MinCostFilterChain(List<ToIntFunction<T>> costFunctions) {
    this.costFunctions = costFunctions;
  }

  @Override
  public Set<T> filter(Set<T> elements) {
    for (ToIntFunction<T> costFunction : costFunctions) {
      elements = filter(elements, costFunction);
    }
    return elements;
  }

  private Set<T> filter(Set<T> elements, ToIntFunction<T> costFunction) {
    var result = new HashSet<T>();
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
}
