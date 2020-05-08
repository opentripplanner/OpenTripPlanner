package org.opentripplanner.transit.raptor.rangeraptor.multicriteria;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals.AbstractStopArrival;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoComparator;


/**
 * The multi-criteria Range Raptor need to keep all pareto-optimal boardings for each pattern
 * while possessing the stops in the pattern. This class keep the needed state for these
 * boardings to avoid recalculating each value more than once and to be able put then in a
 * {@link org.opentripplanner.transit.raptor.util.paretoset.ParetoSet}.
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
    private final int tripSpecificTime;

    public Boarding(
        AbstractStopArrival<T> prevArrival,
        int boardStopIndex,
        int boardPos,
        int boardTime,
        int boardWaitTime,
        int relativeCost,
        T trip
    ) {
        this.prevArrival = prevArrival;
        this.boardStopIndex = boardStopIndex;
        this.boardPos = boardPos;
        this.boardTime = boardTime;
        this.boardWaitTime = boardWaitTime;
        this.tripSpecificTime = trip.departure(0);
        this.trip = trip;
        this.relativeCost = relativeCost;
    }

    /**
     * This is the function used to compare {@link Boarding}s for a given pattern. Since we only
     * compare trip-bordings for a given pattern and a given Raptor round, only 2 criteria is need:
     * <ul>
     *   <li>a time or sequence number for the trip boarded - an early trips is better than a later
     *   one. The first trip departure time is used in this implementation, see the constructor.
     *   <li>the cost for each boarding - this cost is never used in another context, so it does
     *   not need to be specific to a particular place or time - chose whatever is simplest to
     *   compute.
     * </ul>
     */
    public static <T extends RaptorTripSchedule> ParetoComparator<Boarding<T>> paretoComparator() {
        return (l, r) -> l.tripSpecificTime < r.tripSpecificTime || l.relativeCost < r.relativeCost;
    }
}
