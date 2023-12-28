package org.opentripplanner.apis.transmodel.mapping;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.opentripplanner.routing.api.request.PassThroughPoint;
import org.opentripplanner.transit.service.TransitService;

class PassThroughLocationMapper {

  static List<PassThroughPoint> toLocations(
    final TransitService transitService,
    final List<Map<String, Object>> passThroughPoints
  ) {
    return passThroughPoints
      .stream()
      .map(p -> handlePoint(transitService, p))
      .filter(Objects::nonNull)
      .collect(toList());
    // TODO Propagate an error if a stopplace is unknown and fails lookup.
  }

  private static PassThroughPoint handlePoint(
    final TransitService transitService,
    Map<String, Object> map
  ) {
    final List<String> stops = (List<String>) map.get("placeIds");
    final String name = (String) map.get("name");
    if (stops == null) {
      return null;
    }

    return stops
      .stream()
      .map(TransitIdMapper::mapIDToDomain)
      .flatMap(id -> {
        var stopLocations = transitService.getStopOrChildStops(id);
        if (stopLocations.isEmpty()) {
          throw new RuntimeException("No match for %s.".formatted(id));
        }
        return stopLocations.stream();
      })
      .collect(collectingAndThen(toList(), sls -> new PassThroughPoint(sls, name)));
  }
}
