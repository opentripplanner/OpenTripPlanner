package org.opentripplanner.routing.algorithm.transferoptimization.model.passthrough;

import java.util.List;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.PassThroughPoint;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilterFactory;
import org.opentripplanner.routing.algorithm.transferoptimization.model.TripToTripTransfer;

/**
 * Create a filer chain function and find the best transfers combination for journey that also
 * includes all pass-through points.
 * <p>
 * The algorithm starts with the last trip in the journey, then goes backwards looping through all
 * possible transfers for each transit leg. For each possible transfer stop position C2 value is
 * calculated. The filter chain function is going  to use c2 value and cost function to determine
 * whether the tail should be included or excluded from result.
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
public class PassThroughFilterFactory<T extends RaptorTripSchedule>
  implements PathTailFilterFactory<T> {

  private final List<PassThroughPoint> passThroughPoints;
  private final PathTailFilterFactory<T> delegate;

  public PassThroughFilterFactory(
    List<PassThroughPoint> passThroughPoints,
    PathTailFilterFactory<T> delegate
  ) {
    this.passThroughPoints = passThroughPoints;
    this.delegate = delegate;
  }

  @Override
  public PathTailFilter<OptimizedPathTail<T>> createFilter(
    List<List<TripToTripTransfer<T>>> possibleTransfers
  ) {
    var calculator = new PathTailC2Calculator<>(possibleTransfers, passThroughPoints);
    return new PassThroughPathTailFilter<>(
      delegate.createFilter(possibleTransfers),
      calculator::calculateC2
    );
  }
}
