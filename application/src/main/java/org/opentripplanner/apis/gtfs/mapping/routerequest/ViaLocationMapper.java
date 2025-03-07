package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.collection.ListUtils;

/**
 * Maps the input data to the data structure needed for via routing.
 */
class ViaLocationMapper {

  static final String FIELD_STOP_LOCATION_IDS = "stopLocationIds";
  static final String FIELD_LABEL = "label";
  static final String FIELD_MINIMUM_WAIT_TIME = "minimumWaitTime";
  static final String FIELD_VISIT = "visit";
  static final String FIELD_PASS_THROUGH = "passThrough";

  static List<ViaLocation> mapToViaLocations(@Nullable List<Map<String, Map<String, Object>>> via) {
    return ListUtils.nullSafeImmutableList(via)
      .stream()
      .map(ViaLocationMapper::mapViaLocation)
      .toList();
  }

  private static ViaLocation mapViaLocation(Map<String, Map<String, Object>> via) {
    var passThrough = via.get(FIELD_PASS_THROUGH);
    var visit = via.get(FIELD_VISIT);

    if (passThrough != null && passThrough.get(FIELD_STOP_LOCATION_IDS) != null) {
      return new PassThroughViaLocation(
        (String) passThrough.get(FIELD_LABEL),
        mapStopLocationIds((List<String>) passThrough.get(FIELD_STOP_LOCATION_IDS))
      );
    } else if (visit != null) {
      return new VisitViaLocation(
        (String) visit.get(FIELD_LABEL),
        (Duration) visit.get(FIELD_MINIMUM_WAIT_TIME),
        mapStopLocationIds((List<String>) visit.get(FIELD_STOP_LOCATION_IDS)),
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
