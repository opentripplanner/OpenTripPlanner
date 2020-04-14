package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRoute;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTimeTable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TripScheduleExactMatchSearchTest {

    private static final int STOP = 0;

    // The test dummy calculators have a fixed iteration step of 60 seconds
    private static final int ITERATION_STEP = 60;
    private static final int TRIP_TIME = 500;
    private static final boolean FORWARD = true;
    private static final boolean REVERSE = false;
    private static final TestRaptorTripSchedule TRIP_SCHEDULE = TestRaptorTripSchedule
            .create("T1")
            .withBoardAndAlightTimes(TRIP_TIME)
            .build();
    private static final RaptorTimeTable<TestRaptorTripSchedule> TIME_TABLE = new TestRoute(TRIP_SCHEDULE);

    private TripScheduleSearch<TestRaptorTripSchedule> subject;

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
        assertTrue(subject.search(earliestDepartureTime, STOP));

        earliestDepartureTime = TRIP_TIME - ITERATION_STEP + 1;
        assertTrue(subject.search(earliestDepartureTime, STOP));

        earliestDepartureTime = TRIP_TIME + 1;
        assertFalse(subject.search(earliestDepartureTime, STOP));

        earliestDepartureTime = TRIP_TIME - ITERATION_STEP;
        assertFalse(subject.search(earliestDepartureTime, STOP));

        earliestDepartureTime = TRIP_TIME;
        assertFalse(subject.search(earliestDepartureTime, STOP, 0));
    }

    @Test
    public void testReverseSearch() {
        setup(REVERSE);
        int limit;

        limit = TRIP_TIME;
        assertTrue(subject.search(limit, STOP));

        limit = TRIP_TIME + ITERATION_STEP - 1;
        assertTrue(subject.search(limit, STOP));

        limit = TRIP_TIME - 1;
        assertFalse(subject.search(limit, STOP));

        limit = TRIP_TIME + ITERATION_STEP;
        assertFalse(subject.search(limit, STOP));
    }

    @Test
    public void getCandidateTrip() {
        setup(FORWARD);
        subject.search(TRIP_TIME, STOP);
        assertEquals(TRIP_SCHEDULE, subject.getCandidateTrip());
    }

    @Test
    public void getCandidateTripIndex() {
        setup(FORWARD);
        subject.search(TRIP_TIME, STOP);
        assertEquals(0, subject.getCandidateTripIndex());
    }

    @Test
    public void getCandidateTripTime() {
        setup(FORWARD);
        subject.search(TRIP_TIME, STOP);
        assertEquals(TRIP_TIME, subject.getCandidateTripTime());
    }
}