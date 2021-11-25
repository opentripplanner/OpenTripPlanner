package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;
import static org.opentripplanner.transit.raptor._data.transit.TestTripPattern.pattern;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class TransitStopArrivalTest {

    private static final int BOARD_SLACK = 80;

    private static final int ACCESS_TO_STOP = 100;
    private static final int ACCESS_DEPARTURE_TIME = 8 * 60 * 60;
    private static final int ACCESS_DURATION = 300;
    private static final TestTransfer ACCESS_WALK = walk(ACCESS_TO_STOP, ACCESS_DURATION);
    private static final int ACCESS_COST = ACCESS_WALK.generalizedCost();


    private static final int TRANSIT_TO_STOP = 101;
    private static final int TRANSIT_BOARD_TIME = 9 * 60 * 60;
    private static final int TRANSIT_LEG_DURATION = 1200;
    private static final int TRANSIT_ALIGHT_TIME = TRANSIT_BOARD_TIME + TRANSIT_LEG_DURATION;
    private static final int TRANSIT_TRAVEL_DURATION = ACCESS_DURATION + BOARD_SLACK + TRANSIT_LEG_DURATION;
    private static final int TRANSIT_COST = 128000;
    private static final int ROUND = 1;

    private static final RaptorTripSchedule TRANSIT_TRIP = TestTripSchedule
            .schedule(pattern("T1", 0))
            .arrivals(TRANSIT_ALIGHT_TIME)
            .build();

    private static final AccessStopArrival<RaptorTripSchedule> ACCESS_ARRIVAL
            = new AccessStopArrival<>(ACCESS_DEPARTURE_TIME, ACCESS_WALK);

    private final TransitStopArrival<RaptorTripSchedule> subject = new TransitStopArrival<>(
            ACCESS_ARRIVAL.timeShiftNewArrivalTime(TRANSIT_BOARD_TIME - BOARD_SLACK),
            TRANSIT_TO_STOP,
            TRANSIT_ALIGHT_TIME,
            ACCESS_ARRIVAL.cost() + TRANSIT_COST,
            TRANSIT_TRIP
    );


    @Test
    public void round() {
        assertEquals(ROUND, subject.round());
    }

    @Test
    public void stop() {
        assertEquals(TRANSIT_TO_STOP, subject.stop());
    }

    @Test
    public void arrivedByTransit() {
        assertTrue(subject.arrivedByTransit());
        assertFalse(subject.arrivedByTransfer());
        assertFalse(subject.arrivedByAccess());
    }

    @Test
    public void boardStop() {
        assertEquals(ACCESS_TO_STOP, subject.boardStop());
    }

    @Test
    public void arrivalTime() {
        assertEquals(TRANSIT_ALIGHT_TIME, subject.arrivalTime());
    }

    @Test
    public void cost() {
        assertEquals(ACCESS_COST + TRANSIT_COST, subject.cost());
    }

    @Test
    public void trip() {
        assertSame(TRANSIT_TRIP, subject.trip());
    }

    @Test
    public void travelDuration() {
        assertEquals(
                TRANSIT_TRAVEL_DURATION,
                subject.travelDuration()
        );
    }

    @Test
    public void access() {
        assertSame(ACCESS_ARRIVAL.accessPath().access(), subject.previous().accessPath().access());
    }

    @Test
    public void testToString() {
        assertEquals(
                "Transit { round: 1, stop: 101, pattern: BUS T1, arrival-time: 9:20 $1880 }",
                subject.toString()
        );
    }
}