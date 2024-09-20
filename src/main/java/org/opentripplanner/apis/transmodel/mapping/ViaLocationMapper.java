package org.opentripplanner.apis.transmodel.mapping;

import static java.util.stream.Collectors.toList;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.transmodel.model.plan.ViaLocationInputType;
import org.opentripplanner.framework.collection.CollectionUtils;
import org.opentripplanner.routing.api.request.via.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class ViaLocationMapper {

  static List<ViaLocation> mapToViaLocations(final List<Map<String, Object>> via) {
    return via.stream().map(ViaLocationMapper::mapViaLocation).collect(toList());
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
      .map(ViaLocationMapper::mapLegacyPassThroughViaLocation)
      .collect(toList());
  }

  private static ViaLocation mapViaLocation(Map<String, Object> inputMap) {
    Map<String, Object> visit = (Map<String, Object>) inputMap.get(
      ViaLocationInputType.FIELD_VISIT
    );
    Map<String, Object> passThrough = (Map<String, Object>) inputMap.get(
      ViaLocationInputType.FIELD_PASS_THROUGH
    );

    if (CollectionUtils.isEmpty(visit)) {
      if (CollectionUtils.isEmpty(passThrough)) {
        throw new IllegalArgumentException(
          "Either 'visit' or 'passThrough' should be set in 'via' (@oneOf)."
        );
      } else {
        return mapPassThroughViaLocation(passThrough);
      }
    } else {
      if (CollectionUtils.isEmpty(passThrough)) {
        return mapVisitViaLocation(visit);
      } else {
        throw new IllegalArgumentException(
          "Both 'visit' and 'passThrough' can not be set in 'via' (@oneOf)."
        );
      }
    }
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
