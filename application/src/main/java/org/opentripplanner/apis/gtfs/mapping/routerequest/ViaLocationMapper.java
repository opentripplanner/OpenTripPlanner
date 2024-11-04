package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Maps the input data to the data structure needed for via routing.
 */
class ViaLocationMapper {

  private static final String STOP_LOCATION_IDS = "stopLocationIds";
  private static final String LABEL = "label";
  private static final String MINIMUM_WAIT_TIME = "minimumWaitTime";

  static List<ViaLocation> mapToViaLocations(@Nullable List<Map<String, Map<String, Object>>> via) {
    return ListUtils
      .nullSafeImmutableList(via)
      .stream()
      .map(ViaLocationMapper::mapViaLocation)
      .toList();
  }

  private static ViaLocation mapViaLocation(Map<String, Map<String, Object>> via) {
    var passThrough = via.get("passThrough");
    var visit = via.get("visit");

    if (passThrough != null && passThrough.get(STOP_LOCATION_IDS) != null) {
      return new PassThroughViaLocation(
        (String) passThrough.get(LABEL),
        mapStopLocationIds((List<String>) passThrough.get(STOP_LOCATION_IDS))
      );
    } else if (visit != null) {
      return new VisitViaLocation(
        (String) visit.get(LABEL),
        (Duration) visit.get(MINIMUM_WAIT_TIME),
        mapStopLocationIds((List<String>) visit.get(STOP_LOCATION_IDS)),
        List.of()
      );
    } else {
      throw new IllegalArgumentException("ViaLocation must define either pass-through or visit.");
    }
  }

  private static List<FeedScopedId> mapStopLocationIds(List<String> ids) {
    return ids.stream().map(FeedScopedId::parse).toList();
  }
}
