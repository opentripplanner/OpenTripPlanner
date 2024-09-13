package org.opentripplanner.apis.transmodel.mapping;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import org.opentripplanner.routing.api.request.PassThroughViaLocation;
import org.opentripplanner.routing.api.request.ViaLocation;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class ViaLocationMapper {

  static List<ViaLocation> toPassThroughLocations(
    final List<Map<String, Object>> passThroughPoints
  ) {
    return passThroughPoints
      .stream()
      .map(ViaLocationMapper::mapPassThroughViaLocation)
      .collect(toList());
  }

  private static ViaLocation mapPassThroughViaLocation(Map<String, Object> inputMap) {
    final String name = (String) inputMap.get("name");
    final List<FeedScopedId> stopLocationIds =
      ((List<String>) inputMap.get("placeIds")).stream()
        .map(TransitIdMapper::mapIDToDomain)
        .toList();
    return new PassThroughViaLocation(name, stopLocationIds);
  }
}
