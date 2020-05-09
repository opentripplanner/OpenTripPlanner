package org.opentripplanner.transit.raptor._shared;

import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.api.view.TransferLegView;

class Walk extends AbstractStopArrival {

    Walk(
            int round, int stop, int departureTime, int arrivalTime, ArrivalView<TestRaptorTripSchedule> previous
    ) {
        super(round, stop, departureTime, arrivalTime, 1000, previous);
    }

    @Override public boolean arrivedByTransfer() {
        return true;
    }

    @Override public TransferLegView transferLeg() {
        return () -> durationInSeconds();
    }

    private int durationInSeconds() {
        // The duration is a positive number, also when traveling in REVERSE
        return Math.abs(arrivalTime() -  departureTime());
    }
}
