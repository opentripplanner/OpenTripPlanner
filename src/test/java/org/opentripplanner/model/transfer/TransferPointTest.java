package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TransferPointTest implements TransferTestData {

    @Test
    public void getStation() {
        assertEquals(STATION, STATION_POINT.asStationTransferPoint().getStation());
    }

    @Test
    public void getStop() {
        assertEquals(STOP_A, STOP_POINT_A.asStopTransferPoint().getStop());
    }

    @Test
    public void getTrip() {
        assertEquals(TRIP_1, TRIP_POINT_11.asTripTransferPoint().getTrip());
    }

    @Test
    public void getStopPosition() {
        assertEquals(STOP_POSITION_1, TRIP_POINT_11.asTripTransferPoint().getStopPositionInPattern());
        assertEquals(STOP_POSITION_1, ROUTE_POINT_11.asRouteTransferPoint().getStopPositionInPattern());
    }

    @Test
    public void getSpecificityRanking() {
        assertEquals(0, STATION_POINT.getSpecificityRanking());
        assertEquals(1, STOP_POINT_A.getSpecificityRanking());
        assertEquals(2, ROUTE_POINT_11.getSpecificityRanking());
        assertEquals(3, TRIP_POINT_11.getSpecificityRanking());
    }

    @Test
    public void isStationTransferPoint() {
        List.of(STATION_POINT, STOP_POINT_A, ROUTE_POINT_11, TRIP_POINT_11).forEach( p -> {
            assertEquals(p == STATION_POINT, p.isStationTransferPoint());
            assertEquals(p == STOP_POINT_A, p.isStopTransferPoint());
            assertEquals(p == ROUTE_POINT_11, p.isRouteTransferPoint());
            assertEquals(p == TRIP_POINT_11, p.isTripTransferPoint());
        });
    }

    @Test
    public void applyToAllTrips() {
        assertTrue(STATION_POINT.applyToAllTrips());
        assertTrue(STOP_POINT_A.applyToAllTrips());
        assertTrue(ROUTE_POINT_11.applyToAllTrips());
        assertFalse(TRIP_POINT_11.applyToAllTrips());
    }

    @Test
    public void testToString() {
        assertEquals("<Station F:1>", STATION_POINT.toString());
        assertEquals("<Stop F:A>", STOP_POINT_A.toString());
        assertEquals("<Route F:1 @stopPos:1>", ROUTE_POINT_11.toString());
        assertEquals("<Trip F:2 @stopPos:3>", TRIP_POINT_23.toString());
    }
}