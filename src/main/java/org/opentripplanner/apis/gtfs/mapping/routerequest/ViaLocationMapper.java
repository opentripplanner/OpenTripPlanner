package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.transmodel.mapping.TransitIdMapper;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Maps the input data to the data structure needed for via routing.
 */
class ViaLocationMapper {

  private static final String FIELD_LABEL = "label";
  private static final String FIELD_MINIMUM_WAIT_TIME = "minimumWaitTime";
  private static final String FIELD_STOP_LOCATION_IDS = "stopLocationIds";
  private static final String FIELD_VISIT = "visit";
  private static final String FIELD_PASS_THROUGH = "passThrough";

  static List<ViaLocation> mapToViaLocations(final List<Map<String, Object>> via) {
    return via.stream().map(ViaLocationMapper::mapViaLocation).toList();
  }

  private static ViaLocation mapViaLocation(Map<String, Object> inputMap) {
    var fieldName = FIELD_PASS_THROUGH;
    if (inputMap.containsKey(FIELD_VISIT)) {
      fieldName = FIELD_VISIT;
    }

    Map<String, Object> value = (Map<String, Object>) inputMap.get(fieldName);

    return switch (fieldName) {
      case FIELD_VISIT -> mapVisitViaLocation(value);
      case FIELD_PASS_THROUGH -> mapPassThroughViaLocation(value);
      default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
    };
  }

  private static VisitViaLocation mapVisitViaLocation(Map<String, Object> inputMap) {
    var label = (String) inputMap.get(FIELD_LABEL);
    var minimumWaitTime = (Duration) inputMap.get(FIELD_MINIMUM_WAIT_TIME);
    var stopLocationIds = mapStopLocationIds(inputMap);
    return new VisitViaLocation(label, minimumWaitTime, stopLocationIds, List.of());
  }

  private static PassThroughViaLocation mapPassThroughViaLocation(Map<String, Object> inputMap) {
    var label = (String) inputMap.get(FIELD_LABEL);
    var stopLocationIds = mapStopLocationIds(inputMap);
    return new PassThroughViaLocation(label, stopLocationIds);
  }

  private static List<FeedScopedId> mapStopLocationIds(Map<String, Object> map) {
    var c = (Collection<String>) map.get(FIELD_STOP_LOCATION_IDS);
    return c.stream().map(TransitIdMapper::mapIDToDomain).toList();
  }
}
