package org.opentripplanner.model.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TransferPointTest implements TransferTestData {

    public static final int STOP_POSITION_1 = 1;
    public static final int STOP_POSITION_2 = 2;

    private final TransferPoint otherPoint = new RouteTransferPoint(ROUTE_2, TRIP_2, STOP_POSITION_2);


    @Test
    public void getStop() {
        assertEquals(STOP_A, STOP_POINT_A.getStop());
        assertNull(TRIP_POINT_11.getStop());
        assertNull(ROUTE_POINT_11.getStop());
    }

    @Test
    public void getTrip() {
        assertNull(STOP_POINT_A.getTrip());
        assertEquals(TRIP_1, TRIP_POINT_11.getTrip());
        assertEquals(TRIP_1, ROUTE_POINT_11.getTrip());
    }

    @Test
    public void getStopPosition() {
        assertEquals(TransferPoint.NOT_AVAILABLE, STOP_POINT_A.getStopPosition());
        assertEquals(STOP_POSITION_1, TRIP_POINT_11.getStopPosition());
        assertEquals(STOP_POSITION_1, ROUTE_POINT_11.getStopPosition());
    }

    @Test
    public void getSpecificityRanking() {
        assertEquals(0, STOP_POINT_A.getSpecificityRanking());
        assertEquals(1, ROUTE_POINT_11.getSpecificityRanking());
        assertEquals(2, TRIP_POINT_11.getSpecificityRanking());
    }

    @Test
    public void equalsAndHashCode() {
        // A STOP_POINT_A should never match a route or trip point
        assertNotEquals(STOP_POINT_A, ROUTE_POINT_11);
        assertNotEquals(STOP_POINT_A, TRIP_POINT_11);
        assertNotEquals(ROUTE_POINT_11, STOP_POINT_A);
        assertNotEquals(TRIP_POINT_11, STOP_POINT_A);

        assertNotEquals(STOP_POINT_A.hashCode(), ROUTE_POINT_11.hashCode());
        assertNotEquals(STOP_POINT_A.hashCode(), TRIP_POINT_11.hashCode());
        assertNotEquals(ROUTE_POINT_11.hashCode(), STOP_POINT_A.hashCode());
        assertNotEquals(TRIP_POINT_11.hashCode(), STOP_POINT_A.hashCode());

        // If the trip and stopPosition is the same then trip and route point should match
        assertEquals(TRIP_POINT_11, ROUTE_POINT_11);
        assertEquals(ROUTE_POINT_11, TRIP_POINT_11);

        assertEquals(TRIP_POINT_11.hashCode(), ROUTE_POINT_11.hashCode());
        assertEquals(ROUTE_POINT_11.hashCode(), TRIP_POINT_11.hashCode());

        assertNotEquals(TRIP_POINT_11, otherPoint);
        assertNotEquals(ROUTE_POINT_11, otherPoint);
        assertNotEquals(TRIP_POINT_11.hashCode(), otherPoint.hashCode());
        assertNotEquals(ROUTE_POINT_11.hashCode(), otherPoint.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("(stop: F:A)", STOP_POINT_A.toString());
        assertEquals("(route: R:1, trip: T:1, stopPos: 1)", ROUTE_POINT_11.toString());
        assertEquals("(trip: T:1, stopPos: 1)", TRIP_POINT_11.toString());
    }
}