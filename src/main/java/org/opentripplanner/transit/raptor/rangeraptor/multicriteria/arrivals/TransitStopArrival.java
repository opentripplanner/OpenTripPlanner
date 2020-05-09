package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;


import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.TransitLegView;

/**
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TransitStopArrival<T extends RaptorTripSchedule>
    extends AbstractStopArrival<T>
    implements TransitLegView<T>
{
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
    public boolean arrivedByTransit() {
        return true;
    }

    @Override
    public TransitLegView<T> transitLeg() {
        return this;
    }

    public T trip() {
        return trip;
    }

    public int boardStop() {
        return previousStop();
    }
}
