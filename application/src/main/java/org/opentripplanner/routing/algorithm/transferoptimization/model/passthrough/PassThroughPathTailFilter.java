package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import static java.util.stream.Collectors.toSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * Create a filter chain function and find the best combination of transfers for the journey
 * that also includes all pass-through points.
 * <p>
 * The algorithm starts with the last trip in the journey, then goes backwards looping through all
 * possible transfers for each transit leg. For each possible transfer stop position the C2-value
 * is calculated. The filter chain function is going  to use the c2-value and the cost function to
 * determine whether the tail should be included or excluded from result.
 *<p>
 *<b>Example:</b>
 *<p>
 * Let's say we have a trip with 2 transit legs and 3 possible transfer points: AD, BE and CF.
 * <p>
 * There are 3 possible transfer combination with the first and second transit:
 * <pre>
 *    Iteration 1 (initial c2 value is 1 since we have one pass-through point):
 *
 *      ? ~ transit 2 ~ egress | c2 = 1
 *
 *    Iteration 2 (create all possible journey combinations with transfers and calculate c2):
 *
 *      // C2 is 0 since we will pass through E if we board Transit 2 at D
 *      ? ~ transit 1 ~ AD ~ Transit 2 ~ egress | c2 = 0
 *
 *      ? ~ transit 1 ~ BE ~ Transit 2 ~ egress | c2 = 0
 *
 *      // C2 is 1 since we will not pass through E if we board at F
 *      ? ~ transit 1 ~ CF ~ Transit 2 ~ egress | c2 = 1
 *
 *    Iteration 3 (insert access and filter out all combinations where c2 != 0)
 *      access ~ transit 1 ~ AD ~ transit 2 ~ egress | C2 = 0
 *      access ~ transit 1 ~ BE ~ transit 2 ~ egress | C2 = 0
 * </pre>
 * Then we're going to fall back the delegate filter to choose between the two options.
 */
public class PassThroughPathTailFilter<T extends RaptorTripSchedule> implements PathTailFilter<T> {

  private final PathTailFilter<T> filterChain;
  private final PathTailC2Calculator c2Calculator;

  public PassThroughPathTailFilter(
    PathTailFilter<T> filterChain,
    List<RaptorViaLocation> viaLocations
  ) {
    this.filterChain = filterChain;
    this.c2Calculator = new PathTailC2Calculator(viaLocations);
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

  @Override
  public String toString() {
    return ToStringBuilder
      .of(PassThroughPathTailFilter.class)
      .addObj("c2Calculator", c2Calculator)
      .addObj("filterChain", filterChain)
      .toString();
  }
}
