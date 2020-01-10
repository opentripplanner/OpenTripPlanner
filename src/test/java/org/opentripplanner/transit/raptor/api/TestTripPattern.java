package org.opentripplanner.transit.raptor.api;


import org.opentripplanner.transit.raptor.api.transit.TripPatternInfo;

import java.util.Collection;

public class TestTripPattern implements TripPatternInfo<TestTripSchedule> {

    private final TestTripSchedule[] schedules;

    public TestTripPattern(TestTripSchedule... schedules) {
        this.schedules = schedules;
    }

    public TestTripPattern(Collection<TestTripSchedule> schedules) {
        this.schedules = schedules.toArray(new TestTripSchedule[0]);
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
    public TestTripSchedule getTripSchedule(int index) {
        return schedules[index];
    }

    @Override
    public int numberOfTripSchedules() {
        return schedules.length;
    }
}
