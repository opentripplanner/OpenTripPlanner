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
)
  implements PatternRide<T> {
  /**
   * This is the function used to compare {@link PatternRideC2}s for a given pattern.
   * <p>
   * Since Raptor only compare rides for a given pattern and a given Raptor round, only 2 criteria
   * are needed:
   * <ul>
   *   <li>
   *     {@code tripSortIndex} - different trips should not exclude each other. The id can be
   *     any board-/alight-time or sequence number that is uniq for all trips within a pattern.
   *     It is only used to check if two trips are different.
   *   </li>
   *   <li>
   *     {@code relative-cost} of riding a pattern. The cost is used to compare paths that have
   *      boarded the same trip. Two paths riding different trips are not compared, due to the
   *      first criteria. There is several ways to compute this cost, but you must include the
   *      previous-stop-arrival-cost and the additional cost of boarding/riding the pattern.
   *      We assume the cost increase with the same amount for all rides(same trip) traversing
   *      down the pattern; Than we can safely ignore the cost added between each stop; Hence
   *      calculating the "relative" board-cost. Remember to include the cost of transit. You
   *      need to account for the cost of getting from A to B when comparing two
   *      {@link PatternRideC2}s boarding at A and B.
   *   </li>
   * <p>
   */
  public static <T extends RaptorTripSchedule> ParetoComparator<
    PatternRideC2<T>
  > paretoComparatorRelativeCost(DominanceFunction dominanceFunctionC2) {
    return (l, r) ->
      l.tripSortIndex != r.tripSortIndex ||
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
