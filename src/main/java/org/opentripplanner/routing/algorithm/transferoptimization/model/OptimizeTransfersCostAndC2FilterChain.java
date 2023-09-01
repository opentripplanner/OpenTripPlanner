package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OptimizeTransfersCostAndC2FilterChain<T> implements OptimizeTransfersFilterChain<T> {

  private final MinCostFilterChain<T> filterChain;
  private final Function<T, Integer> getC2;

  public OptimizeTransfersCostAndC2FilterChain(
    MinCostFilterChain<T> filterChain,
    Function<T, Integer> getC2
  ) {
    this.filterChain = filterChain;
    this.getC2 = getC2;
  }

  @Override
  public Set<T> filter(Set<T> elements) {
    Map<Integer, Set<T>> elementsByC2Value = elements
      .stream()
      .collect(Collectors.groupingBy(getC2, toSet()));
    Set<T> result = new HashSet<>();
    for (Integer c2 : elementsByC2Value.keySet()) {
      result.addAll(filterChain.filter(elementsByC2Value.get(c2)));
    }
    return result;
  }
}
