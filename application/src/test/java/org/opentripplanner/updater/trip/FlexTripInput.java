package org.opentripplanner.updater.trip;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.time.TimeUtils;

public record FlexTripInput(String id, List<FlexStop> stops) {
  public record FlexStop(StopLocation stop, int windowStart, int windowEnd) {
    public FlexStop(StopLocation stop, String windowStart, String windowEnd) {
      this(stop, TimeUtils.time(windowStart), TimeUtils.time(windowEnd));
    }
  }
  public Route route() {
    return TimetableRepositoryForTest.route("route-1").build();
  }

  public static FlexTripInputBuilder of(String id) {
    return new FlexTripInputBuilder(id);
  }
  public static class FlexTripInputBuilder {

    private final String id;
    private final List<FlexStop> stops = new ArrayList<>();

    FlexTripInputBuilder(String id) {
      this.id = id;
    }

    public FlexTripInputBuilder addStop(StopLocation stop, String windowStart, String windowEnd) {
      this.stops.add(new FlexStop(stop, TimeUtils.time(windowStart), TimeUtils.time(windowEnd)));
      return this;
    }

    public FlexTripInput build() {
      return new FlexTripInput(id, stops);
    }
  }
}
