package org.opentripplanner.transit.raptor._shared;

import org.opentripplanner.transit.raptor.api.view.ArrivalView;
import org.opentripplanner.transit.raptor.api.view.TransitPathView;

public class Bus extends AbstractStopArrival implements TransitPathView<TestRaptorTripSchedule> {
    private final TestRaptorTripSchedule trip;

    public Bus(
            int round,
            int stop,
            int arrivalTime,
            TestRaptorTripSchedule trip,
            ArrivalView<TestRaptorTripSchedule> previous
    ) {
        super(round, stop, arrivalTime, 1000, previous);
        this.trip = trip;
    }
    @Override public boolean arrivedByTransit() { return true; }
    @Override public TransitPathView<TestRaptorTripSchedule> transitLeg() { return this; }
    @Override public int boardStop() { return previous().stop(); }
    @Override public TestRaptorTripSchedule trip() { return trip; }
}
