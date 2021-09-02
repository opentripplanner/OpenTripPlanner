package org.opentripplanner.transit.raptor.rangeraptor.transit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.findTripForwardSearch;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.findTripReverseSearch;

import org.junit.Test;
import org.opentripplanner.transit.raptor._data.RaptorTestConstants;
import org.opentripplanner.transit.raptor._data.stoparrival.Access;
import org.opentripplanner.transit.raptor._data.stoparrival.Bus;
import org.opentripplanner.transit.raptor._data.transit.TestTripPattern;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.path.TransitPathLeg;

public class TripTimesSearchTest implements RaptorTestConstants {

    private static final int A_BOARD_TIME = 110;
    private static final int C_ALIGHT_TIME = 300;
    private static final int A_BOARD_EARLY = A_BOARD_TIME - 10;
    private static final int C_ALIGHT_LATE = C_ALIGHT_TIME + 10;


    // Given a trip-schedule with board-times [110, 210, -] and alight-times [-, 200, 300].
    private TestTripSchedule schedule = TestTripSchedule
            .schedule(pattern("P1", STOP_B, STOP_D, STOP_H))
            .departures(A_BOARD_TIME, 210, 310)
            .arrivals(100, 200, C_ALIGHT_TIME)
            .build();

    @Test
    public void findTripWithPlentySlack() {
        BoarAndAlightTime r;

        // Search AFTER EDT
        r = findTripForwardSearch(busFwd(STOP_B, STOP_H, C_ALIGHT_LATE));

        assertEquals(A_BOARD_TIME, r.boardTime());
        assertEquals(C_ALIGHT_TIME, r.alightTime());

        // Search BEFORE LAT
        r = findTripReverseSearch(busRwd(STOP_H, STOP_B, A_BOARD_EARLY));

        assertEquals(A_BOARD_TIME, r.boardTime());
        assertEquals(C_ALIGHT_TIME, r.alightTime());
    }

    @Test
    public void findTripWithoutSlack() {
        BoarAndAlightTime r;

        // Search AFTER EDT
        r = findTripForwardSearch(busFwd(STOP_B, STOP_H, C_ALIGHT_TIME));

        assertEquals(A_BOARD_TIME, r.boardTime());
        assertEquals(C_ALIGHT_TIME, r.alightTime());


        // Search BEFORE LAT
        r = findTripReverseSearch(busRwd(STOP_H, STOP_B, A_BOARD_TIME));

        assertEquals(A_BOARD_TIME, r.boardTime());
        assertEquals(C_ALIGHT_TIME, r.alightTime());
    }

    @Test
    public void findTripTimes() {
        BoarAndAlightTime r;

        // Search AFTER EDT
        TestTripSchedule trip = TestTripSchedule
            .schedule(TestTripPattern.pattern(STOP_C, STOP_F, STOP_H))
            .times(A_BOARD_TIME, A_BOARD_TIME + 10, C_ALIGHT_TIME)
            .build();

        var leg = new TransitPathLeg<>(STOP_C, A_BOARD_TIME, STOP_H, C_ALIGHT_TIME, -0, trip, null);

        r = TripTimesSearch.findTripTimes(leg);

        assertEquals(A_BOARD_TIME, r.boardTime());
        assertEquals(C_ALIGHT_TIME, r.alightTime());
    }

    @Test
    public void noTripFoundWhenArrivalIsToEarly() {
        try {
            findTripForwardSearch(busFwd(STOP_B, STOP_H, C_ALIGHT_TIME - 1));
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
            findTripReverseSearch(busRwd(STOP_H, STOP_B, A_BOARD_TIME + 1));
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
            findTripForwardSearch(busFwd(STOP_B, STOP_H, 0));
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
    public void noTripFoundWhenReverseArrivalIsWayTooEarly() {
        try {
            findTripReverseSearch(busRwd(STOP_H, STOP_B, 10_000));
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
    public void noTripFoundWhenFromStopIsMissing() {
        try {
            findTripForwardSearch(busFwd(STOP_B, STOP_B, C_ALIGHT_LATE));
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
    public void noTripFoundWhenToStopIsMissingInReverseSearch() {
        try {
            findTripReverseSearch(busRwd(STOP_H, STOP_H, A_BOARD_EARLY));
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
                .schedule(pattern(1, 111, 122, 133, 144, 155, 111, 122, 133, 144, 155, 1155))
                .departures( 10, 110, 210, 310, 410, 510, 610, 710, 810, 910, 1010, 1110)
                .arrivals( 0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100)
                .build();

        BoarAndAlightTime r;

        // TEST FORWARD SEARCH
        {
            // Board in the 2nd loop at stop 2 and get off at stop 3
            r = findTripForwardSearch(busFwd( 122, 133, 800));
            assertEquals(710, r.boardTime());
            assertEquals(800, r.alightTime());

            // Board in the 1st loop at stop 4 and get off at stop 3
            r = findTripForwardSearch(busFwd(144, 133, 800));
            assertEquals(410, r.boardTime());
            assertEquals(800, r.alightTime());

            // Board in the 1st stop, ride the loop twice, alight at the last stop
            r = findTripForwardSearch(busFwd(1, 1155, 1100));
            assertEquals(10, r.boardTime());
            assertEquals(1100, r.alightTime());
        }

        // TEST REVERSE SEARCH
        {
            // Board in the 2nd loop at stop 2 and get off at stop 3
            r = findTripReverseSearch(busRwd(133, 122, 710));
            assertEquals(710, r.boardTime());
            assertEquals(800, r.alightTime());

            // Board in the 1st loop at stop 4 and get off at stop 3
            r = findTripReverseSearch(busRwd(133, 144, 410));
            assertEquals(410, r.boardTime());
            assertEquals(800, r.alightTime());

            // Board in the 1st stop, ride the loop twice, alight at the last stop
            r = findTripReverseSearch(busRwd( 1155, 1, 10));
            assertEquals(10, r.boardTime());
            assertEquals(1100, r.alightTime());
        }
    }

    Bus busFwd(int accessToStop, int transitToStop, int arrivalTime) {
        Access access = new Access(accessToStop, -9999, -9999, -9999);
        return new Bus(1, transitToStop, arrivalTime, -9999, schedule, access);
    }

    Bus busRwd(int accessToStop, int transitToStop, int arrivalTime) {
        Access access = new Access(accessToStop, -9999, -9999, -9999);
        return new Bus(1, transitToStop, arrivalTime, -9999, schedule, access);
    }
}