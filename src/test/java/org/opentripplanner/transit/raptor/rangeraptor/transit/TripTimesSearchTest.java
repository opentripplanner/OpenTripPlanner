package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.stoparrival.Access;
import org.opentripplanner.transit.raptor._data.stoparrival.Bus;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.findTripForwardSearch;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.findTripReverseSearch;

public class TripTimesSearchTest {

    // The route have 3 stops A,B,C with the following indexes:
    public static final int STOP_A_INDEX = 111;
    public static final int STOP_C_INDEX = 333;

    public static final int A_BOARD_TIME = 110;
    public static final int C_ALIGHT_TIME = 300;
    public static final int A_BOARD_EARLY = A_BOARD_TIME - 10;
    public static final int C_ALIGHT_LATE = C_ALIGHT_TIME + 10;


    // Given a trip-schedule with board-times [110, 210, -] and alight-times [-, 200, 300].
    private TestTripSchedule schedule = TestTripSchedule
            .create("T1")
            .withBoardTimes(A_BOARD_TIME, 210, 310)
            .withAlightTimes(100, 200, C_ALIGHT_TIME)
            .withStopIndexes(STOP_A_INDEX, 999, STOP_C_INDEX)
            .build();

    @Test
    public void findTripWithPlentySlack() {
        TripTimesSearch.BoarAlightTimes r;

        // Search AFTER EDT
        r = findTripForwardSearch(busFwd(STOP_A_INDEX, STOP_C_INDEX, C_ALIGHT_LATE));

        assertEquals(A_BOARD_TIME, r.boardTime);
        assertEquals(C_ALIGHT_TIME, r.alightTime);

        // Search BEFORE LAT
        r = findTripReverseSearch(busRwd(STOP_C_INDEX, STOP_A_INDEX, A_BOARD_EARLY));

        assertEquals(A_BOARD_TIME, r.boardTime);
        assertEquals(C_ALIGHT_TIME, r.alightTime);
    }

    @Test
    public void findTripWithoutSlack() {
        TripTimesSearch.BoarAlightTimes r;

        // Search AFTER EDT
        r = findTripForwardSearch(busFwd(STOP_A_INDEX, STOP_C_INDEX, C_ALIGHT_TIME));

        assertEquals(A_BOARD_TIME, r.boardTime);
        assertEquals(C_ALIGHT_TIME, r.alightTime);


        // Search BEFORE LAT
        r = findTripReverseSearch(busRwd(STOP_C_INDEX, STOP_A_INDEX, A_BOARD_TIME));

        assertEquals(A_BOARD_TIME, r.boardTime);
        assertEquals(C_ALIGHT_TIME, r.alightTime);
    }

    @Test
    public void noTripFoundWhenArrivalIsToEarly() {
        try {
            findTripForwardSearch(busFwd(STOP_A_INDEX, STOP_C_INDEX, C_ALIGHT_TIME - 1));
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No stops matching 'toStop'.")
            );
        }
    }

    @Test
    public void noTripFoundWhenReverseArrivalIsToLate() {
        try {
            findTripReverseSearch(busRwd(STOP_C_INDEX, STOP_A_INDEX, A_BOARD_TIME + 1));
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No stops matching 'fromStop'.")
            );
        }
    }

    @Test
    public void noTripFoundWhenArrivalIsWayTooEarly() {
        try {
            findTripForwardSearch(busFwd(STOP_A_INDEX, STOP_C_INDEX, 0));
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                e.getMessage(),
                e.getMessage().contains("No arrivals before 'latestArrivalTime'")
            );
        }
    }

    @Test
    public void noTripFoundWhenReverseArrivalIsWayTooEarly() {
        try {
            findTripReverseSearch(busRwd(STOP_C_INDEX, STOP_A_INDEX, 10_000));
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                e.getMessage(),
                e.getMessage().contains("No departures after 'earliestDepartureTime'")
            );
        }
    }

    @Test
    public void noTripFoundWhenFromStopIsMissing() {
        try {
            findTripForwardSearch(busFwd(STOP_A_INDEX, STOP_A_INDEX, C_ALIGHT_LATE));
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No stops matching 'fromStop'")
            );
        }
    }

    @Test
    public void noTripFoundWhenToStopIsMissingInReverseSearch() {
        try {
            findTripReverseSearch(busRwd(STOP_C_INDEX, STOP_C_INDEX, A_BOARD_EARLY));
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No stops matching 'toStop'")
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
        // stops: Start at 1, loop twice: 111, 122, 133, 144, 155, and end at 1155
        // alight times:    [  -, 100, 200, 300, 400, .., 1100] and
        // departure times: [ 10, 110, 210, 310, 410, .., 1110].
        schedule = TestTripSchedule
                .create("T2")
                .withBoardTimes( 10, 110, 210, 310, 410, 510, 610, 710, 810, 910, 1010, 1110)
                .withAlightTimes( 0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100)
                .withStopIndexes( 1, 111, 122, 133, 144, 155, 111, 122, 133, 144,  155, 1155)
                .build();

        TripTimesSearch.BoarAlightTimes r;

        // TEST FORWARD SEARCH
        {
            // Board in the 2nd loop at stop 2 and get off at stop 3
            r = findTripForwardSearch(busFwd( 122, 133, 800));
            assertEquals(710, r.boardTime);
            assertEquals(800, r.alightTime);

            // Board in the 1st loop at stop 4 and get off at stop 3
            r = findTripForwardSearch(busFwd(144, 133, 800));
            assertEquals(410, r.boardTime);
            assertEquals(800, r.alightTime);

            // Board in the 1st stop, ride the loop twice, alight at the last stop
            r = findTripForwardSearch(busFwd(1, 1155, 1100));
            assertEquals(10, r.boardTime);
            assertEquals(1100, r.alightTime);
        }

        // TEST REVERSE SEARCH
        {
            // Board in the 2nd loop at stop 2 and get off at stop 3
            r = findTripReverseSearch(busRwd(133, 122, 710));
            assertEquals(710, r.boardTime);
            assertEquals(800, r.alightTime);

            // Board in the 1st loop at stop 4 and get off at stop 3
            r = findTripReverseSearch(busRwd(133, 144, 410));
            assertEquals(410, r.boardTime);
            assertEquals(800, r.alightTime);

            // Board in the 1st stop, ride the loop twice, alight at the last stop
            r = findTripReverseSearch(busRwd( 1155, 1, 10));
            assertEquals(10, r.boardTime);
            assertEquals(1100, r.alightTime);
        }
    }

    Bus busFwd(int accessToStop, int transitToStop, int arrivalTime) {
        Access access = new Access(accessToStop, -9999, -9999);
        return new Bus(1, transitToStop, arrivalTime, schedule, access);
    }

    Bus busRwd(int accessToStop, int transitToStop, int arrivalTime) {
        Access access = new Access(accessToStop, -9999, -9999);
        return new Bus(1, transitToStop, arrivalTime, schedule, access);
    }
}