package org.opentripplanner.transit.raptor._shared;

import org.opentripplanner.transit.raptor.api.view.ArrivalView;

class Bus extends AbstractStopArrival {
    private final TestRaptorTripSchedule trip;

    Bus(
            int round, int stop, int departureTime, int arrivalTime, TestRaptorTripSchedule trip,
            ArrivalView<TestRaptorTripSchedule> previous
    ) {
        super(round, stop, departureTime, arrivalTime, 1000, previous);
        this.trip = trip;
    }
    @Override public int boardStop() { return previous().stop(); }
    @Override public TestRaptorTripSchedule trip() { return trip; }
    @Override public boolean arrivedByTransit() { return true; }
}
