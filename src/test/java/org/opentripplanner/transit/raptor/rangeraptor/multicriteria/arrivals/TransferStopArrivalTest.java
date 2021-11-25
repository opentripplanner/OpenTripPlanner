package org.opentripplanner.transit.raptor.rangeraptor.multicriteria.arrivals;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.raptor._data.transit.TestTransfer.walk;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.raptor._data.transit.TestTransfer;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;

public class TransferStopArrivalTest {

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
    private static final int TRANSIT_COST = 128000;
    private static final RaptorTripSchedule TRANSIT_TRIP = null;
    private static final int ROUND = 1;


    private static final int TRANSFER_TO_STOP = 102;
    private static final int TRANSFER_LEG_DURATION = 360;
    private static final int TRANSFER_ALIGHT_TIME = TRANSIT_ALIGHT_TIME + TRANSFER_LEG_DURATION;
    private static final TestTransfer TRANSFER_WALK = walk(TRANSFER_TO_STOP, TRANSFER_LEG_DURATION);
    private static final int TRANSFER_COST = TRANSFER_WALK.generalizedCost();

    private static final int EXPECTED_COST = ACCESS_COST + TRANSIT_COST + TRANSFER_COST;

    private static final AccessStopArrival<RaptorTripSchedule> ACCESS_ARRIVAL
            = new AccessStopArrival<>(ACCESS_DEPARTURE_TIME, ACCESS_WALK);

    private static final TransitStopArrival<RaptorTripSchedule> TRANSIT_ARRIVAL = new TransitStopArrival<>(
            ACCESS_ARRIVAL.timeShiftNewArrivalTime(TRANSIT_BOARD_TIME - BOARD_SLACK),
            TRANSIT_TO_STOP,
            TRANSIT_ALIGHT_TIME,
            ACCESS_ARRIVAL.cost() + TRANSIT_COST,
            TRANSIT_TRIP
    );

    private final TransferStopArrival<RaptorTripSchedule> subject
            = new TransferStopArrival<>(TRANSIT_ARRIVAL, TRANSFER_WALK, TRANSFER_ALIGHT_TIME);

    @Test
    public void arrivedByTransfer() {
        assertTrue(subject.arrivedByTransfer());
        assertFalse(subject.arrivedByTransit());
        assertFalse(subject.arrivedByAccess());
    }

    @Test
    public void stop() {
        assertEquals(TRANSFER_TO_STOP, subject.stop());
    }

    @Test
    public void arrivalTime() {
        assertEquals(TRANSFER_ALIGHT_TIME, subject.arrivalTime());
    }

    @Test
    public void cost() {
        assertEquals(EXPECTED_COST, subject.cost());
    }

    @Test
    public void travelDuration() {
        assertEquals(
                ACCESS_DURATION + BOARD_SLACK + TRANSIT_LEG_DURATION + TRANSFER_LEG_DURATION,
                subject.travelDuration()
        );
    }

    @Test
    public void round() {
        assertEquals(ROUND, subject.round());
    }

    @Test
    public void previous() {
        assertSame(TRANSIT_ARRIVAL, subject.previous());
    }

    @Test
    public void testToString() {
        assertEquals(
                "Walk { round: 1, stop: 102, arrival-time: 9:26 $2600 }",
                subject.toString()
        );
    }

}