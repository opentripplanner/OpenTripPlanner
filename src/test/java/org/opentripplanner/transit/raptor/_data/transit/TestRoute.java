package org.opentripplanner.transit.raptor._data.transit;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntUnaryOperator;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.transfer.TransferConstraint;
import org.opentripplanner.transit.raptor.api.request.SearchDirection;
import org.opentripplanner.transit.raptor.api.transit.RaptorConstrainedTripScheduleBoardingSearch;
import org.opentripplanner.transit.raptor.api.transit.RaptorRoute;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripScheduleSearch;

public class TestRoute implements RaptorRoute<TestTripSchedule>, RaptorTimeTable<TestTripSchedule> {

    private final TestTripPattern pattern;
    private final List<TestTripSchedule> schedules = new ArrayList<>();
    private final TestConstrainedBoardingSearch transferConstraintsForwardSearch =
            new TestConstrainedBoardingSearch(true);
    private final TestConstrainedBoardingSearch transferConstraintsReverseSearch =
            new TestConstrainedBoardingSearch(false);


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
    public IntUnaryOperator getArrivalTimes(int stopPositionInPattern) {
        final int[] arrivalTimes = schedules.stream()
                .mapToInt(schedule -> schedule.arrival(stopPositionInPattern))
                .toArray();
        return (int i) -> arrivalTimes[i];
    }

    @Override
    public IntUnaryOperator getDepartureTimes(int stopPositionInPattern) {
        final int[] departureTimes = schedules.stream()
                .mapToInt(schedule -> schedule.departure(stopPositionInPattern))
                .toArray();
        return (int i) -> departureTimes[i];
    }

    @Override
    public int numberOfTripSchedules() {
        return schedules.size();
    }

    @Override
    public boolean useCustomizedTripSearch() { return false; }

    @Override
    public RaptorTripScheduleSearch<TestTripSchedule> createCustomizedTripSearch(
            SearchDirection direction
    ) {
        throw new IllegalStateException(
                "Support for frequency based trips are not implemented here. " +
                "This is outside the scope of the Raptor unit tests."
        );
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
    public RaptorConstrainedTripScheduleBoardingSearch<TestTripSchedule> transferConstraintsForwardSearch() {
        return transferConstraintsForwardSearch;
    }

    @Override
    public RaptorConstrainedTripScheduleBoardingSearch<TestTripSchedule> transferConstraintsReverseSearch() {
        return transferConstraintsReverseSearch;
    }

    public List<TestConstrainedTransfer> listTransferConstraintsForwardSearch() {
        return transferConstraintsForwardSearch.constrainedBoardings();
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

    void clearTransferConstraints() {
        transferConstraintsForwardSearch.clear();
        transferConstraintsReverseSearch.clear();
    }

    /**
     * Add a transfer constraint to the route by iterating over all trips and matching
     * the provided {@code toTrip}(added to forward search) {@code fromTrip}(added to reverse
     * search) with the rips in the route timetable.
     */
    void addTransferConstraint(
            TestTripSchedule fromTrip,
            int fromStopPos,
            TestTripSchedule toTrip,
            int toStopPos,
            TransferConstraint constraint
    ) {
        for (int i = 0; i < timetable().numberOfTripSchedules(); i++) {
            var trip = timetable().getTripSchedule(i);
            if(toTrip == trip) {
                this.transferConstraintsForwardSearch.addConstraintTransfers(
                        fromTrip, fromStopPos,
                        trip, i, toStopPos,
                        trip.arrival(toStopPos),
                        constraint
                );
            }
            // Reverse search transfer, the {@code source/target} is the trips in order of the
            // reverse search, which is opposite from {@code from/to} in the result path.
            if(fromTrip == trip) {
                this.transferConstraintsReverseSearch.addConstraintTransfers(
                        toTrip, toStopPos,
                        trip, i, fromStopPos,
                        trip.departure(fromStopPos),
                        constraint
                );
            }
        }
    }
}
