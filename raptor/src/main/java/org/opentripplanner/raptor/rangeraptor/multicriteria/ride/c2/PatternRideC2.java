package org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2;

import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.McStopArrival;
import org.opentripplanner.raptor.rangeraptor.multicriteria.ride.PatternRide;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A {@link PatternRide} with support for c1 {@code generalized-cost} and c2 (custom-use-case-cost).
 */
public record PatternRideC2<T extends RaptorTripSchedule>(
  McStopArrival<T> prevArrival,
  int boardStopIndex,
  int boardPos,
  int boardTime,
  int boardC1,
  int relativeC1,
  int c2,
  int tripSortIndex,
  T trip
) implements PatternRide<T> {
  /**
   * See {@link PatternRide} for the pareto comparison strategy used by this comparator.
   */
  public static <T extends RaptorTripSchedule> ParetoComparator<
    PatternRideC2<T>
  > paretoComparatorRelativeCost(DominanceFunction dominanceFunctionC2) {
    return (l, r) ->
      l.tripSortIndex < r.tripSortIndex ||
      l.relativeC1 < r.relativeC1 ||
      dominanceFunctionC2.leftDominateRight(l.c2, r.c2);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(PatternRideC2.class)
      .addNum("prevArrival", prevArrival.stop())
      .addNum("boardStop", boardStopIndex)
      .addNum("boardPos", boardPos)
      .addServiceTime("boardTime", boardTime)
      .addNum("boardC1", boardC1)
      .addNum("relativeC1", relativeC1)
      .addNum("c2", c2)
      .addNum("tripSortIndex", tripSortIndex)
      .addObj("trip", trip)
      .toString();
  }

  @Override
  public PatternRide<T> updateC2(int newC2) {
    return new PatternRideC2<>(
      prevArrival,
      boardStopIndex,
      boardPos,
      boardTime,
      boardC1,
      relativeC1,
      newC2,
      tripSortIndex,
      trip
    );
  }
}
