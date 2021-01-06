package org.opentripplanner.transit.raptor._shared;


import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.util.Collection;

public class TestRoute
        implements RaptorRoute<TestTripSchedule>,
        RaptorTimeTable<TestTripSchedule>
{

    private final TestTripSchedule[] schedules;

    public TestRoute(TestTripSchedule... schedules) {
        this.schedules = schedules;
    }

    public TestRoute(Collection<TestTripSchedule> schedules) {
        this.schedules = schedules.toArray(new TestTripSchedule[0]);
    }

    @Override
    public TestTripSchedule getTripSchedule(int index) {
        return schedules[index];
    }

    @Override
    public int numberOfTripSchedules() {
        return schedules.length;
    }

    @Override
    public RaptorTimeTable<TestTripSchedule> timetable() {
        return this;
    }

    @Override
    public RaptorTripPattern pattern() {
        return schedules[0].pattern();
    }
}
