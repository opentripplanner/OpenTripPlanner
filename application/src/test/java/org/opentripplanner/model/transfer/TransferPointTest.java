package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.transfer.TransferTestData.POS_1;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_1;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_2;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_1A;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_1S;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_2B;
import static org.opentripplanner.model.transfer.TransferTestData.ROUTE_POINT_2S;
import static org.opentripplanner.model.transfer.TransferTestData.STATION;
import static org.opentripplanner.model.transfer.TransferTestData.STATION_POINT;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_A;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_B;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_POINT_A;
import static org.opentripplanner.model.transfer.TransferTestData.STOP_POINT_B;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_11;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_POINT_11_1;
import static org.opentripplanner.model.transfer.TransferTestData.TRIP_POINT_21_3;

import java.util.List;
import org.junit.jupiter.api.Test;

public class TransferPointTest {

  @Test
  public void getStation() {
    assertEquals(STATION, STATION_POINT.asStationTransferPoint().getStation());
    assertEquals(STATION, ROUTE_POINT_1S.asRouteStationTransferPoint().getStation());
  }

  @Test
  public void getStop() {
    assertEquals(STOP_A, STOP_POINT_A.asStopTransferPoint().getStop());
    assertEquals(STOP_A, ROUTE_POINT_1A.asRouteStopTransferPoint().getStop());
    assertEquals(STOP_B, STOP_POINT_B.asStopTransferPoint().getStop());
    assertEquals(STOP_B, ROUTE_POINT_2B.asRouteStopTransferPoint().getStop());
  }

  @Test
  public void getRoute() {
    assertEquals(ROUTE_1, ROUTE_POINT_1S.asRouteStationTransferPoint().getRoute());
    assertEquals(ROUTE_1, ROUTE_POINT_1A.asRouteStopTransferPoint().getRoute());
    assertEquals(ROUTE_2, ROUTE_POINT_2S.asRouteStationTransferPoint().getRoute());
    assertEquals(ROUTE_2, ROUTE_POINT_2B.asRouteStopTransferPoint().getRoute());
  }

  @Test
  public void getTrip() {
    assertEquals(TRIP_11, TRIP_POINT_11_1.asTripTransferPoint().getTrip());
  }

  @Test
  public void getStopPosition() {
    assertEquals(POS_1, TRIP_POINT_11_1.asTripTransferPoint().getStopPositionInPattern());
  }

  @Test
  public void getSpecificityRanking() {
    assertEquals(0, STATION_POINT.getSpecificityRanking());
    assertEquals(1, STOP_POINT_A.getSpecificityRanking());
    assertEquals(2, ROUTE_POINT_1S.getSpecificityRanking());
    assertEquals(3, ROUTE_POINT_1A.getSpecificityRanking());
    assertEquals(4, TRIP_POINT_11_1.getSpecificityRanking());
  }

  @Test
  public void isNnnTransferPoint() {
    List.of(STATION_POINT, STOP_POINT_A, ROUTE_POINT_1A, ROUTE_POINT_1S, TRIP_POINT_11_1).forEach(
      p -> {
        assertEquals(p == STATION_POINT, p.isStationTransferPoint());
        assertEquals(p == STOP_POINT_A, p.isStopTransferPoint());
        assertEquals(p == ROUTE_POINT_1A, p.isRouteStopTransferPoint());
        assertEquals(p == ROUTE_POINT_1S, p.isRouteStationTransferPoint());
        assertEquals(p == TRIP_POINT_11_1, p.isTripTransferPoint());
      }
    );
  }

  @Test
  public void applyToAllTrips() {
    assertTrue(STATION_POINT.appliesToAllTrips());
    assertTrue(STOP_POINT_A.appliesToAllTrips());
    assertTrue(ROUTE_POINT_1A.appliesToAllTrips());
    assertTrue(ROUTE_POINT_1S.appliesToAllTrips());
    assertFalse(TRIP_POINT_11_1.appliesToAllTrips());
  }

  @Test
  public void testToString() {
    assertEquals("StationTP{F:Central Station}", STATION_POINT.toString());
    assertEquals("StopTP{F:A}", STOP_POINT_A.toString());
    assertEquals("RouteTP{F:1, stop F:A}", ROUTE_POINT_1A.toString());
    assertEquals("RouteTP{F:1, station F:Central Station}", ROUTE_POINT_1S.toString());
    assertEquals("TripTP{F:21, stopPos 3}", TRIP_POINT_21_3.toString());
  }
}
