package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoComparator;


/**
 * This class represent the STATE for one possible path up until the point where a given trip is
 * boarded(onboard of a vehicle). It represent the STATE of riding a trip, having boarded at a
 * given stop, but not yet alighted.
 * <p>
 * Instances of this class only exist in the context of a given pattern for a given round. Hence
 * when comparing instances we may assume that they have the same number-of-transfers and the same
 * Pattern. We take advantage of this by excluding all "constant" criteria from the on-board-pattern
 * comparator used by the pareto-set of pattern boardings.
 * <p>
 * This implementation of the multi-criteria Range Raptor keep all pareto-optimal _boardings_ for
 * each pattern while possessing each stops down the line. This class keep the needed state for
 * these boardings to avoid recalculating each value more than once and to be able put then in a
 * {@link org.opentripplanner.transit.raptor.util.paretoset.ParetoSet}.
 * <p>
 * We do not do this the same way as described in the original Raptor paper. The original McRaptor
 * algorithm keep a bag of stop-arrivals  while traversing the pattern. We keep a "bag"
 * ({@link McTransitWorker#patternBoardings}) of {@link Boarding}s for the given pattern. The main
 * differences are:
 * <ul>
 *  <li>
 *    Alight-/arrival specific cost is not included when comparing {@link Boarding}s. This is
 *    ok, since we add this before adding a path to the stop-arrivals at a given stop. This
 *    assumes that the cost of alighting/arrival is the same for all paths arriving by the same
 *    trip. This allow us to eliminate paths, without doing the actual stop-arrival cost
 *    calculation.
 *  </li>
 *  <li>
 *    We do NOT allow a boarding of one trip to exclude the boarding of another trip in the
 *    pareto-set. Two {@link Boarding}s are both optimal, if they have boarded the same pattern, in
 *    the same round, but on different trips/vehicles. This have no measurable impact on
 *    performance, compared with allowing an earlier trip dominating a later one. But, it allows
 *    for a trip to be optimal at some stops, and another trip to be optimal at other stops. This
 *    may happen if the generalized-cost is not <em>increasing</em> with the same amount for
 *    each trip between each stop.
 *  </li>
 *  <li>
 *    We do not have to update all elements in the "pattern-bag" for every stop visited. The
 *    {@code relative-cost} is calculated once - when adding the path to the "pattern-bag"
 *    of {@link Boarding}s.
 *  </li>
 * </ul>
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
final class Boarding<T extends RaptorTripSchedule> {
    final AbstractStopArrival<T> prevArrival;
    final int boardStopIndex;
    final int boardPos;
    final int boardTime;
    final int boardWaitTime;
    final T trip;

    // Pareto vector
    private final int relativeCost;
    private final int tripId;

    public Boarding(
        AbstractStopArrival<T> prevArrival,
        int boardStopIndex,
        int boardPos,
        int boardTime,
        int boardWaitTime,
        int relativeCost,
        T trip,
        int tripId
    ) {
        this.prevArrival = prevArrival;
        this.boardStopIndex = boardStopIndex;
        this.boardPos = boardPos;
        this.boardTime = boardTime;
        this.boardWaitTime = boardWaitTime;
        this.tripId = tripId;
        this.trip = trip;
        this.relativeCost = relativeCost;
    }

    /**
     * This is the function used to compare {@link Boarding}s for a given pattern.
     * <p>
     * Since Raptor only compare trip-boardings for a given pattern and a given Raptor round, only
     * 2 criteria are needed:
     * <ul>
     *   <li>
     *     {@code tripId} - different trips should not exclude each other. The id can be any board-/
     *      alight-time or sequence number that is uniq for all trips within a pattern. It is only
     *      used to check if two trips are different. The pattern trip index is used in this
     *      implementation.
     *   </li>
     *   <li>
     *     {@code relative-cost} of boarding a pattern. The cost is used to compare paths that have
     *      boarded the same trip. Two paths boarding different trips are not compared, due to the
     *      first criteria. There is several ways to compute this cost, but you must include the
     *      previous-stop-arrival-cost and a small additional cost of boarding/riding the pattern.
     *      We assume the cost increase with the same amount for all boardings(same trip) traversing
     *      down the pattern; Than we can safely ignore the cost added between each stop; Hence
     *      calculating the "relative" board-cost. Remember to include the cost of transit. You
     *      need to account for the cost of getting from A to B when comparing two {@link Boarding}s
     *      boarding at A and B.
     *   </li>
     * <p>
     */
    public static <T extends RaptorTripSchedule>
    ParetoComparator<Boarding<T>> paretoComparatorRelativeCost() {
        return (l, r) -> l.tripId != r.tripId || l.relativeCost < r.relativeCost;
    }
}
