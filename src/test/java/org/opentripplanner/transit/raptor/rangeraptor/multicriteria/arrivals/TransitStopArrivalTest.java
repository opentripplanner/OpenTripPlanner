package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import org.junit.Test;
import org.opentripplanner.transit.raptor._shared.TestRaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.transit.raptor.rangeraptor.transit.TransitCalculator.testDummyCalculator;

public class TransitStopArrivalTest {

    private static final int BOARD_SLACK = 80;

    private static final int ACCESS_TO_STOP = 100;
    private static final int ACCESS_DEPARTURE_TIME = 8 * 60 * 60;
    private static final int ACCESS_DURATION = 300;
    private static final int ACCESS_COST = 500;


    private static final int TRANSIT_TO_STOP = 101;
    private static final int TRANSIT_BOARD_TIME = 9 * 60 * 60;
    private static final int TRANSIT_LEG_DURATION = 1200;
    private static final int TRANSIT_ALIGHT_TIME = TRANSIT_BOARD_TIME + TRANSIT_LEG_DURATION;
    private static final int TRANSIT_TRAVEL_DURATION = ACCESS_DURATION + BOARD_SLACK + TRANSIT_LEG_DURATION;
    private static final int TRANSIT_COST = 200;
    private static final RaptorTripSchedule TRANSIT_TRIP = TestRaptorTripSchedule
            .create("T1")
            .withAlightTimes(TRANSIT_ALIGHT_TIME)
            .build();
    private static final int ROUND = 1;

    private static final TransitCalculator TRANSIT_CALCULATOR = testDummyCalculator(true);

    private static final AccessStopArrival<RaptorTripSchedule> ACCESS_ARRIVAL = new AccessStopArrival<>(
            ACCESS_TO_STOP,
            ACCESS_DEPARTURE_TIME,
            ACCESS_DURATION,
            ACCESS_COST,
            null
    );

    private final TransitStopArrival<RaptorTripSchedule> subject = new TransitStopArrival<>(
            ACCESS_ARRIVAL,
            TRANSIT_TO_STOP,
            TRANSIT_ALIGHT_TIME,
            TRANSIT_BOARD_TIME,
            TRANSIT_TRIP,
            TRANSIT_TRAVEL_DURATION,
            TRANSIT_COST
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
        assertFalse(subject.arrivedByAccessLeg());
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
    public void departureTime() {
        assertEquals(TRANSIT_BOARD_TIME, subject.departureTime());
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
    public void previous() {
        assertSame(ACCESS_ARRIVAL, subject.previous());
    }

    @Test
    public void testToString() {
        assertEquals(
                "TransitStopArrival { Rnd: 1, Stop: 101, Time: 9:20 (9:00), Cost: 700 }",
                subject.toString()
        );
    }
}