package org.opentripplanner.model.transfer;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTransferPriority;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.WgsCoordinate;

public interface TransferTestData {
    String FEED_ID = "F";

    Station STATION = new Station(
            createId(1),
            "Central Station",
            new WgsCoordinate(60.0, 11.0),
            null, null, null, null,
            StopTransferPriority.ALLOWED
    );

    int STOP_POSITION_1 = 1;
    int STOP_POSITION_2 = 2;
    int STOP_POSITION_3 = 3;

    Stop STOP_A = Stop.stopForTest("A", 60.0, 11.0);
    Stop STOP_B = Stop.stopForTest("B", 60.0, 11.0);

    Route ROUTE_1 = createRoute(1, "L1");
    Route ROUTE_2 = createRoute(2, "L2");

    Trip TRIP_1 = createTrip(1, ROUTE_1);
    Trip TRIP_2 = createTrip(2, ROUTE_2);

    TransferPoint STATION_POINT = new StationTransferPoint(STATION);

    TransferPoint STOP_POINT_A = new StopTransferPoint(STOP_A);
    TransferPoint STOP_POINT_B = new StopTransferPoint(STOP_B);

    TransferPoint ROUTE_POINT_11 = new RouteTransferPoint(ROUTE_1, STOP_POSITION_1);
    TransferPoint ROUTE_POINT_22 = new RouteTransferPoint(ROUTE_2,  STOP_POSITION_2);

    TransferPoint TRIP_POINT_11 = new TripTransferPoint(TRIP_1, STOP_POSITION_1);
    TransferPoint TRIP_POINT_23 = new TripTransferPoint(TRIP_2, STOP_POSITION_3);

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
