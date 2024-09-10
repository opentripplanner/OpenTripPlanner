package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.routing.api.request.PassThroughPoint;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.service.TransitService;

class PassThroughLocationMapper {

  static List<PassThroughPoint> toLocations(
    final TransitService transitService,
    final List<Map<String, Object>> passThroughPoints
  ) {
    return passThroughPoints.stream().map(p -> handlePoint(transitService, p)).toList();
  }

  private static PassThroughPoint handlePoint(
    final TransitService transitService,
    Map<String, Object> map
  ) {
    Map<String, Object> element = (Map<String, Object>) map.get("passThroughLocation");
    String id = (String) element.get("stopLocationId");

    final String name = (String) element.get("name");
    if (StringUtils.hasNoValue(id)) {
      throw new IllegalArgumentException("No stops in pass-through point");
    }

    var stopLocationId = FeedScopedId.parse(id);
    var stopLocations = List.copyOf(transitService.getStopOrChildStops(stopLocationId));
    if (stopLocations.isEmpty()) {
      throw new IllegalArgumentException("No match for %s.".formatted(id));
    }
    return new PassThroughPoint(stopLocations, name);
  }
}
