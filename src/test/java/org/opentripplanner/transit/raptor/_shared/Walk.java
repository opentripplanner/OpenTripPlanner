package org.opentripplanner.transit.raptor._shared;

import org.opentripplanner.transit.raptor.api.view.ArrivalView;

class Walk extends AbstractStopArrival {
    Walk(
            int round, int stop, int departureTime, int arrivalTime, ArrivalView<TestRaptorTripSchedule> previous
    ) {
        super(round, stop, departureTime, arrivalTime, 1000, previous);
    }
    @Override public boolean arrivedByTransfer() {
        return true;
    }
    @Override public int transferFromStop() {
        return previous().stop();
    }
}
