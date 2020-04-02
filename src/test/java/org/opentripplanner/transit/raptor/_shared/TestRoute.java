package org.opentripplanner.transit.raptor._shared;


import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.util.Collection;

public class TestRoute
        implements RaptorRoute<TestRaptorTripSchedule>,
        RaptorTimeTable<TestRaptorTripSchedule>
{

    private final TestRaptorTripSchedule[] schedules;

    public TestRoute(TestRaptorTripSchedule... schedules) {
        this.schedules = schedules;
    }

    public TestRoute(Collection<TestRaptorTripSchedule> schedules) {
        this.schedules = schedules.toArray(new TestRaptorTripSchedule[0]);
    }

    @Override
    public TestRaptorTripSchedule getTripSchedule(int index) {
        return schedules[index];
    }

    @Override
    public int numberOfTripSchedules() {
        return schedules.length;
    }

    @Override
    public RaptorTimeTable<TestRaptorTripSchedule> timetable() {
        return this;
    }

    @Override
    public RaptorTripPattern pattern() {
        return schedules[0].pattern();
    }
}
