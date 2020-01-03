package org.opentripplanner.transit.raptor._shared;

import org.opentripplanner.transit.raptor.api.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.view.ArrivalView;

class Walk extends AbstractStopArrival {
    Walk(
            int round, int stop, int departureTime, int arrivalTime, ArrivalView<TestTripSchedule> previous
    ) {
        super(round, stop, departureTime, arrivalTime, 100, previous);
    }
    @Override public boolean arrivedByTransfer() {
        return true;
    }
    @Override public int transferFromStop() {
        return previous().stop();
    }
}
