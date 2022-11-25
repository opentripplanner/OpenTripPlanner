package org.opentripplanner.raptor.rangeraptor.multicriteria;

import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.api.view.PatternRideView;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.opentripplanner.raptor.util.paretoset.ParetoComparator;
import org.opentripplanner.raptor.util.paretoset.ParetoSet;

/**
 * This class represent the STATE for one possible path up until the point where a given trip is
 * boarded(onboard of a vehicle). It represent the STATE of riding a trip, having boarded at a given
 * stop, but not yet alighted.
 * <p>
 * Instances of this class only exist in the context of a given pattern for a given round. Hence
 * when comparing instances we may assume that they have the same number-of-transfers and the same
 * Pattern. We take advantage of this by excluding all "constant" criteria from the pattern-ride
 * comparator used by the pareto-set of patternRides.
 * <p>
 * This implementation of the multi-criteria Range Raptor keep all pareto-optimal _rides_ for each
 * pattern while possessing each stops down the line. This class keep the needed state for these
 * rides to avoid recalculating each value more than once and to be able put then in a {@link
 * ParetoSet}.
 * <p>
 * We do not do this the same way as described in the original Raptor paper. The original McRaptor
 * algorithm keep a bag of labels(stop-arrivals) while traversing the pattern. We keep a "bag" of
 * {@code patternRides} in {@link MultiCriteriaRoutingStrategy} for the given pattern. The main
 * differences are:
 * <ul>
 *  <li>
 *    Alight-/arrival specific cost is not included when comparing {@link PatternRide}s. This is
 *    ok, since we add this before adding a path to the stop-arrivals at a given stop. This
 *    assumes that the cost of alighting/arrival is the same for all paths arriving by the same
 *    trip. This allow us to eliminate paths, without doing the actual stop-arrival cost
 *    calculation.
 *  </li>
 *  <li>
 *    We do NOT allow a one trip to exclude the pattern-rides of another trip in the pareto-set.
 *    Two {@link PatternRide}s are both optimal, if they have boarded the same pattern, in
 *    the same round, but on different trips/vehicles. This have no measurable impact on
 *    performance, compared with allowing an earlier trip dominating a later one. But, it allows
 *    for a trip to be optimal at some stops, and another trip to be optimal at other stops. This
 *    may happen if the generalized-cost is not <em>increasing</em> with the same amount for
 *    each trip between each stop.
 *  </li>
 *  <li>
 *    We do not have to update all elements in the "pattern-bag" for every stop visited. The
 *    {@code relative-cost} is calculated once - when adding the path to the "pattern-bag"
 *    of {@link PatternRide}s.
 *  </li>
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public record PatternRide<T extends RaptorTripSchedule>(
  AbstractStopArrival<T> prevArrival,
  int boardStopIndex,
  int boardPos,
  int boardTime,
  int boardCost,
  int relativeCost,
  int tripSortIndex,
  T trip
)
  implements PatternRideView<T> {
  // Pareto vector: [relativeCost, tripSortIndex]

  public PatternRide(
    AbstractStopArrival<T> prevArrival,
    int boardStopIndex,
    int boardPos,
    int boardTime,
    int boardCost,
    int relativeCost,
    T trip
  ) {
    this(
      prevArrival,
      boardStopIndex,
      boardPos,
      boardTime,
      boardCost,
      relativeCost,
      trip.tripSortIndex(),
      trip
    );
  }

  /**
   * This is the function used to compare {@link PatternRide}s for a given pattern.
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
   *      need to account for the cost of getting from A to B when comparing two {@link PatternRide}s
   *      boarding at A and B.
   *   </li>
   * <p>
   */
  public static <
    T extends RaptorTripSchedule
  > ParetoComparator<PatternRide<T>> paretoComparatorRelativeCost() {
    return (l, r) -> l.tripSortIndex != r.tripSortIndex || l.relativeCost < r.relativeCost;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(PatternRide.class)
      .addNum("prevArrival", prevArrival.stop())
      .addNum("boardStop", boardStopIndex)
      .addNum("boardPos", boardPos)
      .addServiceTime("boardTime", boardTime)
      .addNum("boardCost", boardCost)
      .addNum("relativeCost", relativeCost)
      .addNum("tripSortIndex", tripSortIndex)
      .addObj("trip", trip)
      .toString();
  }
}
