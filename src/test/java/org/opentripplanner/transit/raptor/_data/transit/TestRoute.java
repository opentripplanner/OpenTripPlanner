package org.opentripplanner.transit.raptor._data.transit;


import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestRoute implements RaptorRoute<TestTripSchedule>, RaptorTimeTable<TestTripSchedule> {

    private final TestTripPattern pattern;
    private final List<TestTripSchedule> schedules = new ArrayList<>();

    private TestRoute(TestTripPattern pattern) {
        this.pattern = pattern;
    }

    public static TestRoute route(TestTripPattern pattern){
        return new TestRoute(pattern);
    }

    public static TestRoute route(String name, int ... stopIndexes){
        return route(TestTripPattern.pattern(name, stopIndexes));
    }

    @Override
    public TestTripSchedule getTripSchedule(int index) {
        return schedules.get(index);
    }

    @Override
    public int numberOfTripSchedules() {
        return schedules.size();
    }

    @Override
    public RaptorTimeTable<TestTripSchedule> timetable() {
        return this;
    }

    @Override
    public RaptorTripPattern pattern() {
        return pattern;
    }

    public TestRoute withTimetable(TestTripSchedule ... trips) {
        Collections.addAll(schedules, trips);
        return this;
    }

    public TestRoute withTimetable(TestTripSchedule.Builder ... scheduleBuilders) {
        for (TestTripSchedule.Builder builder : scheduleBuilders) {
            var tripSchedule = builder.pattern(pattern).build();
            schedules.add(tripSchedule);
        }
        return this;
    }
}
