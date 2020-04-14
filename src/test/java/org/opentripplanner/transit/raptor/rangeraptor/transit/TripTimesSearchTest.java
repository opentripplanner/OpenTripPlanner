package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.searchAfterEDT;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.searchBeforeLAT;

public class TripTimesSearchTest {

    // The route have 3 stops A,B,C with the following indexes:
    public static final int STOP_A_INDEX = 111;
    public static final int STOP_C_INDEX = 333;

    // The stop between A and C have the following stop position in the pattern
    public static final int STOP_A_POS = 0;
    public static final int STOP_C_POS = 2;

    public static final int A_BOARD_TIME = 110;
    public static final int C_ALIGHT_TIME = 300;
    public static final int A_BOARD_EARLY = A_BOARD_TIME - 10;
    public static final int C_ALIGHT_LATE = C_ALIGHT_TIME + 10;


    // Given a trip-schedule with board-times [110, 210, -] and alight-times [-, 200, 300].
    private TestRaptorTripSchedule schedule = TestRaptorTripSchedule
            .create("T1")
            .withBoardTimes(A_BOARD_TIME, 210, 310)
            .withAlightTimes(100, 200, C_ALIGHT_TIME)
            .withStopIndexes(STOP_A_INDEX, 999, STOP_C_INDEX)
            .build();

    @Test
    public void findTripWithPlentySlack() {
        TripTimesSearch.Result r;

        // Search AFTER EDT
        r = searchAfterEDT(schedule, STOP_A_INDEX, STOP_C_INDEX, A_BOARD_EARLY);

        assertEquals(A_BOARD_TIME, r.boardTime);
        assertEquals(C_ALIGHT_TIME, r.alightTime);

        // Search BEFORE LAT
        r = searchBeforeLAT(schedule, STOP_A_INDEX, STOP_C_INDEX, C_ALIGHT_LATE);

        assertEquals(A_BOARD_TIME, r.boardTime);
        assertEquals(C_ALIGHT_TIME, r.alightTime);
    }

    @Test
    public void findTripWithoutSlack() {
        TripTimesSearch.Result r;

        // Search AFTER EDT
        r = searchAfterEDT(schedule, STOP_A_INDEX, STOP_C_INDEX, A_BOARD_TIME);

        assertEquals(A_BOARD_TIME, r.boardTime);
        assertEquals(C_ALIGHT_TIME, r.alightTime);


        // Search BEFORE LAT
        r = searchBeforeLAT(schedule, STOP_A_INDEX, STOP_C_INDEX, C_ALIGHT_TIME);

        assertEquals(A_BOARD_TIME, r.boardTime);
        assertEquals(C_ALIGHT_TIME, r.alightTime);
    }

    @Test
    public void noTripFoundWhenDepartureIsToLate() {
        try {
            int oneSecondAfterLastDeparture = schedule.departure(STOP_C_POS) + 1;
            searchAfterEDT(schedule, STOP_A_INDEX, STOP_C_INDEX, oneSecondAfterLastDeparture);
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No departures after earliest-departure-time")
            );
        }
    }

    @Test
    public void noTripFoundWhenArrivalIsToEarly() {
        try {
            int oneSecondBeforeFirstArrival = schedule.arrival(STOP_A_POS) - 1;
            searchBeforeLAT(schedule, STOP_A_INDEX, STOP_C_INDEX, oneSecondBeforeFirstArrival);
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No arrivals before latest-arrival-time")
            );
        }
    }

    @Test
    public void noTripFoundMatchingFromStop() {
        try {
            searchAfterEDT(schedule, STOP_A_INDEX, STOP_C_INDEX, A_BOARD_TIME + 1);
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("No stops matching 'fromStop'"));
        }
    }

    @Test
    public void noTripFoundMatchingToStop() {
        try {
            searchBeforeLAT(schedule, STOP_A_INDEX, STOP_C_INDEX, C_ALIGHT_TIME -1);
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("No stops matching 'toStop'"));
        }
    }

    @Test
    public void noTripFoundWhenToStopIsMissing() {
        try {
            searchAfterEDT(schedule, STOP_A_INDEX, STOP_A_INDEX, A_BOARD_EARLY);
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No stops matching 'toStop'")
            );
        }
    }

    @Test
    public void noTripFoundWhenFromStopIsMissing() {
        try {
            searchBeforeLAT(schedule, STOP_C_INDEX, STOP_C_INDEX, C_ALIGHT_LATE);
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No stops matching 'fromStop'")
            );
        }
    }

    /**
     * The trip-schedule may visit the same stop many times. For example in the case of a
     * subway-loop.
     */
    @Test
    public void findTripWhenScheduleLoops() {
        // Create a trip schedule that run in a 2 loops with a stop before and after the loop
        // stops: Start at 111, loop twice: 11, 22, 33, 44, 55, and end at 555
        // alight times:    [  -, 100, 200, 300, 400 ...] and
        // departure times: [ 10, 110, 210, 310, 410 ...].

        TestRaptorTripSchedule schedule = TestRaptorTripSchedule
                .create("T2")
                .withBoardTimes(10, 110, 210, 310, 410, 510, 610, 710, 810, 910, 1010, 1110)
                .withAlightTimes(0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100)
                .withStopIndexes(111, 11, 22, 33, 44, 55, 11, 22, 33, 44, 55, 555)
                .build();

        TripTimesSearch.Result r;

        // TEST THE searchAfterEDT(...)
        {
            // Board in the 2nd loop at stop 2 and get off at stop 3
            r = searchAfterEDT(schedule, 22, 33, 600);
            assertEquals(710, r.boardTime);
            assertEquals(800, r.alightTime);

            // Board in the 1st loop at stop 4 and get off at stop 3
            r = searchAfterEDT(schedule, 44, 33, 400);
            assertEquals(410, r.boardTime);
            assertEquals(800, r.alightTime);

            // Board in the 1st stop, ride the loop twice, alight at the last stop
            r = searchAfterEDT(schedule, 111, 555, 10);
            assertEquals(10, r.boardTime);
            assertEquals(1100, r.alightTime);
        }

        // REPEAT THE SAME TESTS, BUT USE THE searchBeforeLAT(...) METHOD INSTEAD
        {
            // Board in the 2nd loop at stop 2 and get off at stop 3
            r = searchBeforeLAT(schedule, 22, 33, 800);
            assertEquals(710, r.boardTime);
            assertEquals(800, r.alightTime);

            // Board in the 1st loop at stop 4 and get off at stop 3
            r = searchBeforeLAT(schedule, 44, 33, 800);
            assertEquals(410, r.boardTime);
            assertEquals(800, r.alightTime);

            // Board in the 1st stop, ride the loop twice, alight at the last stop
            r = searchBeforeLAT(schedule, 111, 555, 1100);
            assertEquals(10, r.boardTime);
            assertEquals(1100, r.alightTime);
        }
    }
}