package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.transit.TestRoute;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
        TransitCalculator calculator = TransitCalculator.testDummyCalculator(forward);
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
        assertTrue(subject.search(earliestDepartureTime, STOP_POS_0));

        earliestDepartureTime = TRIP_TIME - ITERATION_STEP + 1;
        assertTrue(subject.search(earliestDepartureTime, STOP_POS_0));

        earliestDepartureTime = TRIP_TIME + 1;
        assertFalse(subject.search(earliestDepartureTime, STOP_POS_0));

        earliestDepartureTime = TRIP_TIME - ITERATION_STEP;
        assertFalse(subject.search(earliestDepartureTime, STOP_POS_0));

        earliestDepartureTime = TRIP_TIME;
        assertFalse(subject.search(earliestDepartureTime, STOP_POS_0, 0));
    }

    @Test
    public void testReverseSearch() {
        setup(REVERSE);
        int limit;

        limit = TRIP_TIME;
        assertTrue(subject.search(limit, STOP_POS_0));

        limit = TRIP_TIME + ITERATION_STEP - 1;
        assertTrue(subject.search(limit, STOP_POS_0));

        limit = TRIP_TIME - 1;
        assertFalse(subject.search(limit, STOP_POS_0));

        limit = TRIP_TIME + ITERATION_STEP;
        assertFalse(subject.search(limit, STOP_POS_0));
    }

    @Test
    public void getCandidateTrip() {
        setup(FORWARD);
        subject.search(TRIP_TIME, STOP_POS_0);
        assertEquals(TRIP_SCHEDULE, subject.getCandidateTrip());
    }

    @Test
    public void getCandidateTripIndex() {
        setup(FORWARD);
        subject.search(TRIP_TIME, STOP_POS_0);
        assertEquals(0, subject.getCandidateTripIndex());
    }

    @Test
    public void getCandidateTripTime() {
        setup(FORWARD);
        subject.search(TRIP_TIME, STOP_POS_0);
        assertEquals(TRIP_TIME, subject.getCandidateTripTime());
    }
}