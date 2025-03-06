package org.opentripplanner.routing.algorithm.transferoptimization.services;

import java.util.HashSet;
import java.util.Set;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.routing.algorithm.transferoptimization.model.OptimizedPathTail;
import org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class takes a list of transit legs and returns the best leg based on the {@link
 * org.opentripplanner.routing.algorithm.transferoptimization.model.PathTailFilter} and
 * the earliest-boarding-time. The filter is used to pick the best leg from the legs which can be
 * boarded after the earliest-boarding-time.
 * <p>
 * HOW IT WORKS
 * <p>
 * The initial set of elements are put in the "reminding" set, and the "selected" set is empty.
 * <p>
 * Each time the {@link #next(int)} method is called the reminding elements are search for
 * candidates where boarding is possible. The new candidates are added to the selected set and the
 * selected set is filtered, and are elements dropped. Dropped elements represent none optimal
 * paths.
 * <p>
 * Elements in the reminding set which can not be boarded is kept in the remaining set for the next
 * call to the {@link #next(int)} method.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
class TransitPathLegSelector<T extends RaptorTripSchedule> {

  private final PathTailFilter<T> filter;
  private Set<OptimizedPathTail<T>> remindingLegs;
  private Set<OptimizedPathTail<T>> selectedLegs;

  private int prevStopPosition = Integer.MAX_VALUE;

  TransitPathLegSelector(final PathTailFilter<T> filter, final Set<OptimizedPathTail<T>> legs) {
    this.filter = filter;
    this.remindingLegs = Set.copyOf(legs);
    this.selectedLegs = new HashSet<>();
  }

  Set<OptimizedPathTail<T>> next(final int fromStopPosition) {
    if (fromStopPosition > prevStopPosition) {
      throw new IllegalStateException(
        "The next method must be called with decreasing time limits. " +
        "fromStopPosition=" +
        fromStopPosition +
        ", previousStopPosition=" +
        prevStopPosition
      );
    }
    prevStopPosition = fromStopPosition;

    Set<OptimizedPathTail<T>> candidates = new HashSet<>();
    Set<OptimizedPathTail<T>> rest = new HashSet<>();

    for (OptimizedPathTail<T> it : remindingLegs) {
      if (fromStopPosition < it.head().toStopPos()) {
        candidates.add(it);
      } else {
        rest.add(it);
      }
    }

    if (candidates.isEmpty()) {
      return selectedLegs;
    }

    candidates.addAll(selectedLegs);

    // Set state
    remindingLegs = rest;
    selectedLegs = filter.filterIntermediateResult(candidates, fromStopPosition);

    return selectedLegs;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TransitPathLegSelector.class)
      .addObj("filter", filter)
      .addCol("remindingLegs", remindingLegs)
      .addCol("selectedLegs", selectedLegs)
      .addNum("prevStopPosition", prevStopPosition)
      .toString();
  }
}
