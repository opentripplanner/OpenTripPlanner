package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator.testDummyCalculator;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestRoute;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;

public class TripScheduleExactMatchSearchTest implements RaptorTestConstants {

    // The test dummy calculators have a fixed iteration step of 60 seconds
    private static final int ITERATION_STEP = 60;
    private static final int TRIP_TIME = 500;
    private static final boolean FORWARD = true;
    private static final boolean REVERSE = false;
    private static final TestTripSchedule TRIP_SCHEDULE = TestTripSchedule
            .schedule()
            .times(TRIP_TIME)
            .build();
    private static final TestRoute TIME_TABLE = TestRoute.route("R1", STOP_A)
        .withTimetable(TRIP_SCHEDULE);

    private TripScheduleSearch<TestTripSchedule> subject;

    public void setup(boolean forward) {
        TransitCalculator<TestTripSchedule> calculator = testDummyCalculator(forward);
        subject = calculator.createExactTripSearch(TIME_TABLE);
    }

    @Test
    public void testForwardSearch() {
        // Given:
        //   A forward search and a fixed trip departure time (TRIP_TIME = 500)
        //   To test this, we change this earliest departure time .
        setup(FORWARD);
        int earliestDepartureTime;

        earliestDepartureTime = TRIP_TIME;
        assertNotNull(subject.search(earliestDepartureTime, STOP_POS_0));

        earliestDepartureTime = TRIP_TIME - ITERATION_STEP + 1;
        assertNotNull(subject.search(earliestDepartureTime, STOP_POS_0));

        earliestDepartureTime = TRIP_TIME + 1;
        assertNull(subject.search(earliestDepartureTime, STOP_POS_0));

        earliestDepartureTime = TRIP_TIME - ITERATION_STEP;
        assertNull(subject.search(earliestDepartureTime, STOP_POS_0));

        earliestDepartureTime = TRIP_TIME;
        assertNull(subject.search(earliestDepartureTime, STOP_POS_0, 0));
    }

    @Test
    public void testReverseSearch() {
        setup(REVERSE);
        int limit;

        limit = TRIP_TIME;
        assertNotNull(subject.search(limit, STOP_POS_0));

        limit = TRIP_TIME + ITERATION_STEP - 1;
        assertNotNull(subject.search(limit, STOP_POS_0));

        limit = TRIP_TIME - 1;
        assertNull(subject.search(limit, STOP_POS_0));

        limit = TRIP_TIME + ITERATION_STEP;
        assertNull(subject.search(limit, STOP_POS_0));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void getCandidateTrip() {
        setup(FORWARD);
        var r = subject.search(TRIP_TIME, STOP_POS_0);
        assertEquals(TRIP_SCHEDULE, r.getTrip());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void getCandidateTripIndex() {
        setup(FORWARD);
        var r = subject.search(TRIP_TIME, STOP_POS_0);
        assertEquals(0, r.getTripIndex());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void getCandidateTripTime() {
        setup(FORWARD);
        var r = subject.search(TRIP_TIME, STOP_POS_0);
        assertEquals(TRIP_TIME, r.getTime());
    }
}