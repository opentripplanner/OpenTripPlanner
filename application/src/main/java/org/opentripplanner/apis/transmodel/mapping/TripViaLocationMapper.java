package org.opentripplanner.apis.transmodel.mapping;

import static java.util.stream.Collectors.toList;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.transmodel.model.plan.TripQuery;
import org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType;
import org.opentripplanner.apis.transmodel.support.OneOfInputValidator;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TripViaLocationMapper {

  static List<ViaLocation> mapToViaLocations(final List<Map<String, Object>> via) {
    return via.stream().map(TripViaLocationMapper::mapViaLocation).collect(toList());
  }

  /**
   * @deprecated Legacy passThrough, use via instead
   */
  @Deprecated
  static List<ViaLocation> toLegacyPassThroughLocations(
    final List<Map<String, Object>> passThroughPoints
  ) {
    return passThroughPoints
      .stream()
      .map(TripViaLocationMapper::mapLegacyPassThroughViaLocation)
      .collect(toList());
  }

  private static ViaLocation mapViaLocation(Map<String, Object> inputMap) {
    var fieldName = OneOfInputValidator.validateOneOf(
      inputMap,
      TripQuery.TRIP_VIA_PARAMETER,
      ViaLocationInputType.FIELD_VISIT,
      ViaLocationInputType.FIELD_PASS_THROUGH
    );

    Map<String, Object> value = (Map<String, Object>) inputMap.get(fieldName);

    return switch (fieldName) {
      case ViaLocationInputType.FIELD_VISIT -> mapVisitViaLocation(value);
      case ViaLocationInputType.FIELD_PASS_THROUGH -> mapPassThroughViaLocation(value);
      default -> throw new IllegalArgumentException("Unknown field: " + fieldName);
    };
  }

  private static VisitViaLocation mapVisitViaLocation(Map<String, Object> inputMap) {
    var label = (String) inputMap.get(ViaLocationInputType.FIELD_LABEL);
    var minimumWaitTime = (Duration) inputMap.get(ViaLocationInputType.FIELD_MINIMUM_WAIT_TIME);
    var stopLocationIds = mapStopLocationIds(inputMap);
    return new VisitViaLocation(label, minimumWaitTime, stopLocationIds, List.of());
  }

  private static PassThroughViaLocation mapPassThroughViaLocation(Map<String, Object> inputMap) {
    var label = (String) inputMap.get(ViaLocationInputType.FIELD_LABEL);
    var stopLocationIds = mapStopLocationIds(inputMap);
    return new PassThroughViaLocation(label, stopLocationIds);
  }

  private static List<FeedScopedId> mapStopLocationIds(Map<String, Object> map) {
    var c = (Collection<String>) map.get(ViaLocationInputType.FIELD_STOP_LOCATION_IDS);
    return c.stream().map(TransitIdMapper::mapIDToDomain).toList();
  }

  /**
   * @deprecated Legacy passThrough, use via instead
   */
  @Deprecated
  private static ViaLocation mapLegacyPassThroughViaLocation(Map<String, Object> inputMap) {
    final String name = (String) inputMap.get("name");
    final List<FeedScopedId> stopLocationIds =
      ((List<String>) inputMap.get("placeIds")).stream()
        .map(TransitIdMapper::mapIDToDomain)
        .toList();
    return new PassThroughViaLocation(name, stopLocationIds);
  }
}
