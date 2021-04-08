package org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.view;

import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.api.view.TransferPathView;
import org.opentripplanner.transit.raptor.rangeraptor.standard.stoparrivals.StopArrivalState;

final class Transfer<T extends RaptorTripSchedule>
    extends StopArrivalViewAdapter<T>
    implements TransferPathView
{
    private final StopArrivalState<T> arrival;
    private final StopsCursor<T> cursor;

    Transfer(int round, int stop, StopArrivalState<T> arrival, StopsCursor<T> cursor) {
        super(round, stop);
        this.arrival = arrival;
        this.cursor = cursor;
    }

    @Override
    public int arrivalTime() {
        return arrival.time();
    }

    @Override
    public boolean arrivedByTransfer() {
        return true;
    }

    @Override
    public TransferPathView transferPath() {
        return this;
    }

    @Override
    public int durationInSeconds() {
        return arrival.transferDuration();
    }

    @Override
    public RaptorTransfer transfer() {
        return arrival.transferPath();
    }

    @Override
    public ArrivalView<T> previous() {
        return cursor.transit(round(), arrival.transferFromStop());
    }

}
