package org.opentripplanner.updater.trip;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * A simple data structure that is used by the {@link RealtimeTestEnvironment} to create
 * trips, trips on date and patterns.
 */
public record TripInput(String id, Route route, List<StopCall> stops) {
  public static TripInputBuilder of(String id) {
    return new TripInputBuilder(id);
  }

  public static class TripInputBuilder implements RealtimeTestConstants {

    private final String id;
    private final List<StopCall> stops = new ArrayList<>();
    // can be made configurable if needed
    private Route route = ROUTE_1;

    TripInputBuilder(String id) {
      this.id = id;
    }

    public TripInputBuilder addStop(RegularStop stopId, String arrivalTime, String departureTime) {
      this.stops.add(
          new StopCall(stopId, TimeUtils.time(arrivalTime), TimeUtils.time(departureTime))
        );
      return this;
    }

    public TripInput build() {
      return new TripInput(id, route, stops);
    }

    public TripInputBuilder withRoute(Route route) {
      this.route = route;
      return this;
    }
  }

  record StopCall(RegularStop stop, int arrivalTime, int departureTime) {}
}
