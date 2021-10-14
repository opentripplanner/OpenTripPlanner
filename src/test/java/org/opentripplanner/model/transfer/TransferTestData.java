package org.opentripplanner.model.transfer;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;

public interface TransferTestData {
    Stop STOP_A = Stop.stopForTest("A", 60.0, 11.0);
    Stop STOP_B = Stop.stopForTest("B", 60.0, 11.0);

    Route ROUTE_1 = new Route(new FeedScopedId("R", "1"));
    Route ROUTE_2 = new Route(new FeedScopedId("R", "2"));

    Trip TRIP_1 = createTrip("1", ROUTE_1);
    Trip TRIP_2 = createTrip("2", ROUTE_2);

    TransferPoint STOP_POINT_A = new StopTransferPoint(STOP_A);
    TransferPoint STOP_POINT_B = new StopTransferPoint(STOP_B);

    TransferPoint ROUTE_POINT_11 = new RouteTransferPoint(ROUTE_1, TRIP_1, 1);
    TransferPoint ROUTE_POINT_22 = new RouteTransferPoint(ROUTE_2, TRIP_2, 2);

    TransferPoint TRIP_POINT_11 = new TripTransferPoint(TRIP_1, 1);
    TransferPoint TRIP_POINT_23 = new TripTransferPoint(TRIP_2, 3);


    private static Trip createTrip(String id, Route route) {
        Trip t = new Trip(new FeedScopedId("T", id));
        t.setRoute(route);
        return t;
    }
}
