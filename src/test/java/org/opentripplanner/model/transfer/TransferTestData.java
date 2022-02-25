package org.opentripplanner.model.transfer;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;

public class TransferTestData {
    static final String FEED_ID = "F";

    static final Station STATION = Station.stationForTest("Central Station", 60.0, 11.0);

    static final int POS_1 = 1;
    static final int POS_2 = 2;
    static final int POS_3 = 3;
    static final int ANY_POS = 999;

    static final Stop STOP_A = Stop.stopForTest("A", 60.0, 11.0);
    static final Stop STOP_B = Stop.stopForTest("B", 60.0, 11.0);
    static final Stop STOP_S = Stop.stopForTest("S", 60.0, 11.0);
    static final Stop ANY_STOP = Stop.stopForTest("any", 60.0, 11.0);

    static final Route ROUTE_1 = createRoute(1, "L1");
    static final Route ROUTE_2 = createRoute(2, "L2");
    static final Route ANY_ROUTE = createRoute(999, "any");

    static final Trip TRIP_11 = createTrip(11, ROUTE_1);
    static final Trip TRIP_12 = createTrip(12, ROUTE_1);
    static final Trip TRIP_21 = createTrip(21, ROUTE_2);
    static final Trip TRIP_22 = createTrip(22, ROUTE_2);
    static final Trip ANY_TRIP = createTrip(999, ANY_ROUTE);

    static final TransferPoint STATION_POINT = new StationTransferPoint(STATION);

    static final TransferPoint STOP_POINT_A = new StopTransferPoint(STOP_A);
    static final TransferPoint STOP_POINT_B = new StopTransferPoint(STOP_B);

    static final TransferPoint ROUTE_POINT_1S = new RouteStationTransferPoint(ROUTE_1, STATION);
    static final TransferPoint ROUTE_POINT_2S = new RouteStationTransferPoint(ROUTE_2, STATION);

    static final TransferPoint ROUTE_POINT_1A = new RouteStopTransferPoint(ROUTE_1, STOP_A);
    static final TransferPoint ROUTE_POINT_2B = new RouteStopTransferPoint(ROUTE_2, STOP_B);

    static final TransferPoint TRIP_POINT_11_1 = new TripTransferPoint(TRIP_11, POS_1);
    static final TransferPoint TRIP_POINT_21_3 = new TripTransferPoint(TRIP_21, POS_3);

    static {
        STATION.addChildStop(STOP_A);
        STOP_A.setParentStation(STATION);
        STATION.addChildStop(STOP_S);
        STOP_S.setParentStation(STATION);
    }

    private static Trip createTrip(int id, Route route) {
        Trip t = new Trip(createId(id));
        t.setRoute(route);
        return t;
    }

    private static Route createRoute(int id, String name) {
        Route r = new Route(createId(id));
        r.setShortName(name);
        return r;
    }

    private static FeedScopedId createId(int id) {
        return new FeedScopedId(FEED_ID, String.valueOf(id));
    }
}
