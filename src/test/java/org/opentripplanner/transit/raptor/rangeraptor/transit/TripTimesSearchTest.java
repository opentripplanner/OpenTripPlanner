package org.opentripplanner.transit.raptor.rangeraptor.transit;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TripTimesSearch.getTripTimes;

public class TripTimesSearchTest {

    // The route have 3 stops A,B,C with the following indexes:
    public static final int STOP_A = 1;
    public static final int STOP_C = 3;

    public static final int BOARD_A = 110;
    public static final int ALIGHT_C = 300;
    public static final int BOARD_A_EARLY = 100;
    public static final int ALIGHT_C_LATE = 330;


    // Given a trip-schedule with board-times [110, 210, -] and alight-times [-, 200, 300].
    private TestRaptorTripSchedule schedule = TestRaptorTripSchedule
            .create("T1")
            .withArrivalTimes(100, 200, 300)
            .withDepartureDelay(10)
            .build();

    @Test
    public void findTripWithPlentySlack() {
        TripTimesSearch.Result r = getTripTimes(schedule,
                STOP_A,
                BOARD_A_EARLY,
                STOP_C,
                ALIGHT_C_LATE
        );

        assertEquals(BOARD_A, r.boardTime);
        assertEquals(ALIGHT_C, r.alightTime);
    }

    @Test
    public void findTripWithoutSlack() {
        TripTimesSearch.Result r = getTripTimes(schedule, STOP_A, BOARD_A, STOP_C, ALIGHT_C);

        assertEquals(BOARD_A, r.boardTime);
        assertEquals(ALIGHT_C, r.alightTime);
    }

    @Test
    public void noTripFoundWhenDepartureIsToLate() {
        try {
            int oneSecondAfterLastDeparture = schedule.departure(2) + 1;
            getTripTimes(schedule, STOP_A, oneSecondAfterLastDeparture, STOP_C, ALIGHT_C_LATE);
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
    public void noTripFoundMatchingFromStop() {
        try {
            getTripTimes(schedule, STOP_A, BOARD_A + 1, STOP_C, ALIGHT_C_LATE);
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("No stops matching fromStop"));
        }
    }

    @Test
    public void noTripFoundWhenArrivalISToEarly() {
        try {
            getTripTimes(schedule, STOP_A, BOARD_A_EARLY, STOP_C, ALIGHT_C - 1);
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
    public void noTripFoundWhenToStopIsMissing() {
        try {
            getTripTimes(schedule, STOP_A, BOARD_A_EARLY, STOP_A, ALIGHT_C);
            fail();
        }
        catch (IllegalStateException e) {
            assertTrue(
                    e.getMessage(),
                    e.getMessage().contains("No stops matching toStop")
            );
        }
    }


    /**
     * The trip-schedule may visit the same stop many times. For example in the case of a
     * subway-loop.
     */
    @Test
    public void findTripWhenScheduleLoops() {
        // Create a trip schedule that run in a loop with 5 stops: [1..5]
        // continuously for 20 rounds with
        // alight times:    [  -, 100, 200, 300, 400 ...] and
        // departure times: [ 10, 110, 210, 310, 410 ...].
        RaptorTripSchedule schedule = new RaptorTripSchedule() {
            RaptorTripPattern pattern = new RaptorTripPattern() {
                @Override public int stopIndex(int stopPositionInPattern) {
                    return 1 + stopPositionInPattern % 5;
                }
                @Override public int numberOfStopsInPattern() { return 5 * 20; }
            };
            @Override public int arrival(int stopPosInPattern) { return stopPosInPattern * 100; }
            @Override public int departure(int stopPosInPattern) {
                return arrival(stopPosInPattern) + 10;
            }
            @Override public String debugInfo() { return "Schedule Loop"; }
            @Override public RaptorTripPattern pattern() { return pattern; }
        };

        TripTimesSearch.Result r;

        // Board in the 3rd loop at stop 2 and get off at stop 3
        r = getTripTimes(schedule, 2, 1000, 3, 1500);
        assertEquals(1110, r.boardTime);
        assertEquals(1200, r.alightTime);

        // Board in the 2rd loop at stop 4 and get off at stop 3 (3rd loop)
        r = getTripTimes(schedule, 4, 500, 3, 1500);
        assertEquals(810, r.boardTime);
        assertEquals(1200, r.alightTime);
    }
}