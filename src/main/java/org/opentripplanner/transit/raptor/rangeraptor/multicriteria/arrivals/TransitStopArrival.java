package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;


import org.opentripplanner.transit.raptor.api.transit.TripScheduleInfo;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public final class TransitStopArrival<T extends TripScheduleInfo> extends AbstractStopArrival<T> {
    private final T trip;

    public TransitStopArrival(AbstractStopArrival<T> previousState, int stopIndex, int arrivalTime, int boardTime, T trip, int travelDuration, int additionalCost) {
        super(
                previousState,
                stopIndex,
                boardTime,
                arrivalTime,
                travelDuration,
                additionalCost
        );
        this.trip = trip;
    }

    @Override
    public boolean arrivedByTransit() {
        return true;
    }

    @Override
    public T trip() {
        return trip;
    }

    @Override
    public int boardStop() {
        return previousStop();
    }
}
