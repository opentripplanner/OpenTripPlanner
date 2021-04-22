package org.opentripplanner.transit.raptor._data.transit;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.transit.raptor.api.transit.RaptorGuaranteedTransferProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

public class TestRoute implements RaptorRoute<TestTripSchedule>, RaptorTimeTable<TestTripSchedule> {

    private final TestTripPattern pattern;
    private final List<TestTripSchedule> schedules = new ArrayList<>();
    private final TestTransferProvider transfersFrom = new TestTransferProvider();
    private final TestTransferProvider transfersTo = new TestTransferProvider();


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

    @Override
    public RaptorGuaranteedTransferProvider<TestTripSchedule> getGuaranteedTransfersTo() {
        return transfersTo;
    }

    @Override
    public RaptorGuaranteedTransferProvider<TestTripSchedule> getGuaranteedTransfersFrom() {
        return transfersFrom;
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

    @Override
    public String toString() {
        return ToStringBuilder.of(TestRoute.class)
                .addObj("pattern", pattern)
                .addObj("schedules", schedules)
                .toString();
    }

    void addGuaranteedTxFrom(
            TestTripSchedule fromTrip,
            int fromTripIndex,
            int fromStopPos,
            TestTripSchedule toTrip,
            int toStopPos
    ) {
        int fromTime = fromTrip.arrival(fromStopPos);
        this.transfersFrom.addGuaranteedTransfers(
                toTrip, toStopPos, fromTrip, fromTripIndex, fromStopPos, fromTime
        );
    }

    void addGuaranteedTxTo(
            TestTripSchedule fromTrip,
            int fromStopPos,
            TestTripSchedule toTrip,
            int toTripIndex,
            int toStopPos
            ) {
        final int toTime = toTrip.departure(toStopPos);
        // This is used in the revers search
        this.transfersTo.addGuaranteedTransfers(
                fromTrip, fromStopPos, toTrip, toTripIndex, toStopPos, toTime
        );
    }
}
