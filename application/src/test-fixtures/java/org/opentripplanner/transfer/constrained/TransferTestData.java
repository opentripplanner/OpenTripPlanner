package org.opentripplanner.transfer.constrained;

import org.opentripplanner.transfer.constrained.model.RouteStationTransferPoint;
import org.opentripplanner.transfer.constrained.model.RouteStopTransferPoint;
import org.opentripplanner.transfer.constrained.model.StationTransferPoint;
import org.opentripplanner.transfer.constrained.model.StopTransferPoint;
import org.opentripplanner.transfer.constrained.model.TransferPoint;
import org.opentripplanner.transfer.constrained.model.TripTransferPoint;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.timetable.Trip;

public class TransferTestData {

  private static TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  public static final Station STATION = TEST_MODEL.station("Central Station").build();

  public static final int POS_1 = 1;
  public static final int POS_2 = 2;
  public static final int POS_3 = 3;
  public static final int ANY_POS = 999;

  public static final RegularStop STOP_A = TEST_MODEL.stop("A", 60.0, 11.0)
    .build()
    .copy()
    .withParentStation(STATION)
    .build();
  public static final RegularStop STOP_B = TEST_MODEL.stop("B", 60.0, 11.0).build();
  public static final RegularStop STOP_S = TEST_MODEL.stop("S", 60.0, 11.0)
    .build()
    .copy()
    .withParentStation(STATION)
    .build();
  public static final RegularStop ANY_STOP = TEST_MODEL.stop("any", 60.0, 11.0).build();

  public static final Route ROUTE_1 = TimetableRepositoryForTest.route("1").build();
  public static final Route ROUTE_2 = TimetableRepositoryForTest.route("2").build();
  public static final Route ANY_ROUTE = TimetableRepositoryForTest.route("ANY").build();

  public static final Trip TRIP_11 = TimetableRepositoryForTest.trip("11")
    .withRoute(ROUTE_1)
    .build();
  public static final Trip TRIP_12 = TimetableRepositoryForTest.trip("12")
    .withRoute(ROUTE_1)
    .build();
  public static final Trip TRIP_21 = TimetableRepositoryForTest.trip("21")
    .withRoute(ROUTE_2)
    .build();
  public static final Trip ANY_TRIP = TimetableRepositoryForTest.trip("999")
    .withRoute(ANY_ROUTE)
    .build();

  public static final TransferPoint STATION_POINT = new StationTransferPoint(STATION);

  public static final TransferPoint STOP_POINT_A = new StopTransferPoint(STOP_A);
  public static final TransferPoint STOP_POINT_B = new StopTransferPoint(STOP_B);

  public static final TransferPoint ROUTE_POINT_1S = new RouteStationTransferPoint(
    ROUTE_1,
    STATION
  );
  public static final TransferPoint ROUTE_POINT_2S = new RouteStationTransferPoint(
    ROUTE_2,
    STATION
  );

  public static final TransferPoint ROUTE_POINT_1A = new RouteStopTransferPoint(ROUTE_1, STOP_A);
  public static final TransferPoint ROUTE_POINT_2B = new RouteStopTransferPoint(ROUTE_2, STOP_B);

  public static final TransferPoint TRIP_POINT_11_1 = new TripTransferPoint(TRIP_11, POS_1);
  public static final TransferPoint TRIP_POINT_21_3 = new TripTransferPoint(TRIP_21, POS_3);
}
