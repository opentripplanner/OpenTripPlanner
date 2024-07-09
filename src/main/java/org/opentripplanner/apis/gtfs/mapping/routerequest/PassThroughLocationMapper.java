package org.opentripplanner.apis.gtfs.mapping.routerequest;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
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
    final List<String> stops = (List<String>) map.get("placeIds");
    final String name = (String) map.get("name");
    if (stops == null || stops.isEmpty()) {
      throw new IllegalArgumentException("No stops in pass-through point");
    }

    return stops
      .stream()
      .map(FeedScopedId::parse)
      .flatMap(id -> {
        var stopLocations = transitService.getStopOrChildStops(id);
        if (stopLocations.isEmpty()) {
          throw new IllegalArgumentException("No match for %s.".formatted(id));
        }
        return stopLocations.stream();
      })
      .collect(collectingAndThen(toList(), sls -> new PassThroughPoint(sls, name)));
  }
}
