package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;


import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransitStopArrival<T extends RaptorTripSchedule> extends AbstractStopArrival<T> {
    private final T trip;

    public TransitStopArrival(
        AbstractStopArrival<T> previousState,
        int stopIndex,
        int boardTime,
        int arrivalTime,
        int additionalCost,
        T trip
    ) {
        super(
                previousState,
                stopIndex,
                boardTime,
                arrivalTime,
                additionalCost
        );
        this.trip = trip;
    }

    @Override
    public RaptorTransfer accessEgress() {
        throw new UnsupportedOperationException("No accessEgress for transit stop arrival");
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
