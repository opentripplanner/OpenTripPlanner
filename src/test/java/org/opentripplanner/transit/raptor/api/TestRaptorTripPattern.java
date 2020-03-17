package org.opentripplanner.transit.raptor.api;


import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.util.Collection;

public class TestRaptorTripPattern implements RaptorTripPattern<TestRaptorTripSchedule> {

    private final TestRaptorTripSchedule[] schedules;

    public TestRaptorTripPattern(TestRaptorTripSchedule... schedules) {
        this.schedules = schedules;
    }

    public TestRaptorTripPattern(Collection<TestRaptorTripSchedule> schedules) {
        this.schedules = schedules.toArray(new TestRaptorTripSchedule[0]);
    }

    @Override
    public int stopIndex(int stopPositionInPattern) {
        return stopPositionInPattern + 1;
    }

    @Override
    public int numberOfStopsInPattern() {
        return schedules[0].size();
    }

    @Override
    public TestRaptorTripSchedule getTripSchedule(int index) {
        return schedules[index];
    }

    @Override
    public int numberOfTripSchedules() {
        return schedules.length;
    }
}
