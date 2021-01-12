package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.api.view.TransitPathView;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;

final class Transit<T extends RaptorTripSchedule>
    extends StopArrivalViewAdapter<T>
    implements TransitPathView<T>
{
    private final StopArrivalState<T> arrival;
    private final StopsCursor<T> cursor;

    Transit(int round, int stop, StopArrivalState<T> arrival, StopsCursor<T> cursor) {
        super(round, stop);
        this.arrival = arrival;
        this.cursor = cursor;
    }

    @Override
    public int arrivalTime() {
        return arrival.transitTime();
    }

    @Override
    public boolean arrivedByTransit() {
        return true;
    }

    @Override
    public TransitPathView<T> transitPath() {
        return this;
    }

    @Override
    public int boardStop() {
        return arrival.boardStop();
    }

    @Override
    public T trip() {
        return arrival.trip();
    }

    @Override
    public ArrivalView<T> previous() {
        return cursor.stop(round()-1, boardStop(), this);
    }

    public int boardTime() {
        return arrival.boardTime();
    }
}
