package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.PassThroughPoint;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;

class PassThroughPathTailFilter<T extends RaptorTripSchedule> implements PathTailFilter<T> {

  private final PathTailFilter<T> filterChain;
  private final PathTailC2Calculator c2Calculator;

  public PassThroughPathTailFilter(
    PathTailFilter<T> filterChain,
    List<PassThroughPoint> passThroughPoints
  ) {
    this.filterChain = filterChain;
    this.c2Calculator = new PathTailC2Calculator(passThroughPoints);
  }

  @Override
  public Set<OptimizedPathTail<T>> filterIntermediateResult(
    Set<OptimizedPathTail<T>> elements,
    int boardStopPosition
  ) {
    Map<Integer, Set<OptimizedPathTail<T>>> elementsByC2Value = elements
      .stream()
      .collect(
        Collectors.groupingBy(
          it -> c2Calculator.calculateC2AtStopPos(it, boardStopPosition),
          toSet()
        )
      );
    var result = new HashSet<OptimizedPathTail<T>>();
    for (var set : elementsByC2Value.values()) {
      result.addAll(filterChain.filterIntermediateResult(set, boardStopPosition));
    }
    return result;
  }

  @Override
  public Set<OptimizedPathTail<T>> filterFinalResult(Set<OptimizedPathTail<T>> elements) {
    Set<OptimizedPathTail<T>> result = elements
      .stream()
      .peek(c2Calculator::calculateC2)
      .filter(it -> it.head().c2() == 0)
      .collect(toSet());

    return filterChain.filterFinalResult(result);
  }
}
