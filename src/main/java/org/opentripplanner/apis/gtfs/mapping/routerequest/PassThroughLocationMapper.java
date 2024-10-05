package org.opentripplanner.apis.gtfs.mapping.routerequest;

import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.collection.CollectionUtils;
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
    Map<String, Object> element = (Map<String, Object>) map.get("passThrough");
    List<String> ids = (List<String>) element.get("stopLocationIds");

    final String name = (String) element.get("name");
    if (CollectionUtils.isEmpty(ids)) {
      throw new IllegalArgumentException("No stops in pass-through point");
    }

    var stopLocations = ids
      .stream()
      .map(FeedScopedId::parse)
      .flatMap(id -> transitService.getStopOrChildStops(id).stream())
      .toList();
    if (stopLocations.isEmpty()) {
      throw new IllegalArgumentException("No stop locations found for %s.".formatted(ids));
    }
    return new PassThroughPoint(stopLocations, name);
  }
}
