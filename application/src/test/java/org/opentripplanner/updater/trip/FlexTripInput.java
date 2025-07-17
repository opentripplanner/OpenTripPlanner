package org.opentripplanner.updater.trip;

import java.util.List;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.time.TimeUtils;

public record FlexTripInput(String id, List<FlexCall> stops) {
  public record FlexCall(StopLocation stop, int windowStart, int windowEnd) {
    public FlexCall(StopLocation stop, String windowStart, String windowEnd) {
      this(stop, TimeUtils.time(windowStart), TimeUtils.time(windowEnd));
    }
  }

  public Route route() {
    return TimetableRepositoryForTest.route("route-1").build();
  }
}
