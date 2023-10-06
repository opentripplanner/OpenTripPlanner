package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;

class PassThroughPathTailFilter<T> implements PathTailFilter<T> {

  private final PathTailFilter<T> filterChain;
  private final Function<T, Integer> getC2;

  public PassThroughPathTailFilter(PathTailFilter<T> filterChain, Function<T, Integer> getC2) {
    this.filterChain = filterChain;
    this.getC2 = getC2;
  }

  @Override
  public Set<T> filterIntermediateResult(Set<T> elements) {
    Map<Integer, Set<T>> elementsByC2Value = elements
      .stream()
      .collect(Collectors.groupingBy(getC2, toSet()));
    Set<T> result = new HashSet<>();
    for (Integer c2 : elementsByC2Value.keySet()) {
      result.addAll(filterChain.filterIntermediateResult(elementsByC2Value.get(c2)));
    }
    return result;
  }

  @Override
  public Set<T> filterFinalResult(Set<T> elements) {
    return elements.stream().filter(tail -> getC2.apply(tail) == 0).collect(toSet());
  }
}
