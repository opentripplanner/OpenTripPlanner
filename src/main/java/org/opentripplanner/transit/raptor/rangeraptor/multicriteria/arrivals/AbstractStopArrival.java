package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.util.paretoset.ParetoComparator;


/**
 * Abstract super class for multi-criteria stop arrival.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public abstract class AbstractStopArrival<T extends RaptorTripSchedule> implements ArrivalView<T> {

    public static <T extends RaptorTripSchedule> ParetoComparator<AbstractStopArrival<T>> compareArrivalTimeRoundAndCost() {
        // This is important with respect to performance. Using the short-circuit logical OR(||) is
        // faster than bitwise inclusive OR(|) (even between boolean expressions)
        return (l, r) -> l.arrivalTime < r.arrivalTime || l.paretoRound < r.paretoRound || l.cost < r.cost;
    }

    public static <T extends RaptorTripSchedule> ParetoComparator<AbstractStopArrival<T>> compareArrivalTimeAndRound() {
        return (l, r) -> l.arrivalTime < r.arrivalTime || l.paretoRound < r.paretoRound;
    }

    private final AbstractStopArrival<T> previous;

    /**
     * We want transits to dominate transfers so we increment the round not only between
     * RangeRaptor rounds, but for transits and transfers also. The access path is paretoRound 0,
     * the first transit path is 1. The following transfer path, if it exist, is paretoRound 2, and
     * the next transit is 3, and so on.
     * <p/>
     * The relationship between Range Raptor round and paretoRound can be described by this formula:
     * <pre>
     *     Range Raptor round =  (paretoRound + 1) / 2
     * </pre>
     */
    private final int paretoRound;
    private final int stop;
    private final int arrivalTime;
    private final int travelDuration;
    private final int cost;

    /**
     * Transit or transfer.
     *
     * @param previous the previous arrival visited for the current trip
     * @param stop stop index for this arrival
     * @param arrivalTime the arrival time for this stop index
     * @param additionalCost the accumulated cost at this stop arrival
     */
    AbstractStopArrival(AbstractStopArrival<T> previous, int stop, int arrivalTime, int additionalCost) {
        this.previous = previous;
        this.paretoRound = previous.paretoRound + (isTransitFollowedByTransit() ? 2 : 1);
        this.stop = stop;
        this.arrivalTime = arrivalTime;
        this.travelDuration = previous.travelDuration + (arrivalTime - previous.arrivalTime);
        this.cost = previous.cost + additionalCost;
    }

    /**
     * Initial state - first stop visited during the RAPTOR algorithm.
     */
    AbstractStopArrival(int stop, int departureTime, int travelDuration, int initialCost, int paretoRound) {
        this.previous = null;
        this.paretoRound = paretoRound;
        this.stop = stop;
        this.arrivalTime = departureTime + travelDuration;
        this.travelDuration = travelDuration;
        this.cost = initialCost;
    }


    @Override
    public final int round() {
        return (paretoRound + 1) / 2;
    }

    @Override
    public final int stop() {
        return stop;
    }

    @Override
    public final int arrivalTime() {
        return arrivalTime;
    }

    public int travelDuration() {
        return travelDuration;
    }

    public AbstractStopArrival<T> timeShiftNewArrivalTime(int newArrivalTime) {
        throw new UnsupportedOperationException("No accessEgress for transfer stop arrival");
    }

    /**
     * @return previous state or throw a NPE if no previousArrival exist.
     */
    final int previousStop() {
        return previous.stop;
    }

    @Override
    public final AbstractStopArrival<T> previous() {
        return previous;
    }

    public int cost() {
        return cost;
    }

    @Override
    public String toString() {
        return asString();
    }

    @Override
    public final boolean equals(Object o) {
        throw new IllegalStateException("Avoid using hashCode() and equals() for this class.");
    }

    @Override
    public final int hashCode() {
        throw new IllegalStateException("Avoid using hashCode() and equals() for this class.");
    }

    private boolean isTransitFollowedByTransit() {
        return arrivedByTransit() && previous.arrivedByTransit();
    }
}
