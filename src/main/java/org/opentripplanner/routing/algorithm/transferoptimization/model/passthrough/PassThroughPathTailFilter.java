package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;

class PassThroughPathTailFilter<T extends RaptorTripSchedule> implements PathTailFilter<T> {

  private final PathTailFilter<T> filterChain;
  private final Function<OptimizedPathTail<T>, Integer> getC2;

  public PassThroughPathTailFilter(
    PathTailFilter<T> filterChain,
    Function<OptimizedPathTail<T>, Integer> getC2
  ) {
    this.filterChain = filterChain;
    this.getC2 = getC2;
  }

  @Override
  public Set<OptimizedPathTail<T>> filterIntermediateResult(Set<OptimizedPathTail<T>> elements) {
    Map<Integer, Set<OptimizedPathTail<T>>> elementsByC2Value = elements
      .stream()
      .collect(Collectors.groupingBy(getC2, toSet()));
    Set<OptimizedPathTail<T>> result = new HashSet<>();
    for (Integer c2 : elementsByC2Value.keySet()) {
      result.addAll(filterChain.filterIntermediateResult(elementsByC2Value.get(c2)));
    }
    return result;
  }

  @Override
  public Set<OptimizedPathTail<T>> filterFinalResult(Set<OptimizedPathTail<T>> elements) {
    return elements.stream().filter(tail -> getC2.apply(tail) == 0).collect(toSet());
  }
}
