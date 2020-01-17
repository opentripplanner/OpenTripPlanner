package org.opentripplanner.transit.raptor.api;


import org.opentripplanner.transit.raptor.api.transit.TripPatternInfo;

import java.util.Collection;

public class TestTripPattern implements TripPatternInfo<TestRaptorTripSchedule> {

    private final TestRaptorTripSchedule[] schedules;

    public TestTripPattern(TestRaptorTripSchedule... schedules) {
        this.schedules = schedules;
    }

    public TestTripPattern(Collection<TestRaptorTripSchedule> schedules) {
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
