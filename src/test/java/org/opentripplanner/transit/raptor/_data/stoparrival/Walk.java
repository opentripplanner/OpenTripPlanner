package org.opentripplanner.transit.raptor._data.stoparrival;

import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.api.view.TransferPathView;

class Walk extends AbstractStopArrival {
    private final int durationInSeconds;

    Walk(
        int round,
        int stop,
        int departureTime,
        int arrivalTime,
        ArrivalView<TestTripSchedule> previous
    ) {
        super(round, stop, arrivalTime, 1000, previous);
        // In a reverse search we the arrival is before the departure
        this.durationInSeconds = Math.abs(arrivalTime - departureTime);
    }

    @Override public boolean arrivedByTransfer() {
        return true;
    }

    @Override public TransferPathView transferPath() {
        return () -> durationInSeconds;
    }
}
