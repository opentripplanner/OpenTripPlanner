package org.opentripplanner.updater.trip;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;

public record RealtimeTripInput(String id, Route route, List<StopCall> stops) {
  public static RealtimeTripInputBuilder of(String id) {
    return new RealtimeTripInputBuilder(id);
  }

  public static class RealtimeTripInputBuilder implements RealtimeTestConstants {

    private final String id;
    private final List<StopCall> stops = new ArrayList<>();
    // can be made configurable if needed
    private final Route route = ROUTE_1;

    RealtimeTripInputBuilder(String id) {
      this.id = id;
    }

    public RealtimeTripInputBuilder addStop(
      RegularStop stopId,
      String arrivalTime,
      String departureTime
    ) {
      this.stops.add(
          new StopCall(stopId, TimeUtils.time(arrivalTime), TimeUtils.time(departureTime))
        );
      return this;
    }

    public RealtimeTripInput build() {
      return new RealtimeTripInput(id, route, stops);
    }
  }

  static record StopCall(RegularStop stop, int arrivalTime, int departureTime) {}
}
