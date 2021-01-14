package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;


import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.TransitPathView;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransitStopArrival<T extends RaptorTripSchedule>
    extends AbstractStopArrival<T>
    implements TransitPathView<T>
{
    private final T trip;

    public TransitStopArrival(
        AbstractStopArrival<T> previousState,
        int stopIndex,
        int arrivalTime,
        int additionalCost,
        T trip
    ) {
        super(
                previousState,
                stopIndex,
                arrivalTime,
                additionalCost
        );
        this.trip = trip;
    }

    @Override
    public boolean arrivedByTransit() {
        return true;
    }

    @Override
    public TransitPathView<T> transitPath() {
        return this;
    }

    public T trip() {
        return trip;
    }

    public int boardStop() {
        return previousStop();
    }
}
