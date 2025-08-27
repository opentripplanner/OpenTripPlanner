package org.opentripplanner.apis.transmodel.mapping;

import java.util.List;
import java.util.Map;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.JourneyRequest;

class ViaSegmentMapper {

  private final TransitFilterNewWayMapper transitFilterNewWayMapper;

  ViaSegmentMapper(FeedScopedIdMapper idMapper) {
    transitFilterNewWayMapper = new TransitFilterNewWayMapper(idMapper);
  }

  JourneyRequest mapViaSegment(RouteRequest defaultRequest, Map<String, Object> viaSegment) {
    var journey = defaultRequest.journey().copyOf();
    if (viaSegment.containsKey("modes")) {
      Map<String, Object> modesInput = (Map<String, Object>) viaSegment.get("modes");
      journey.setModes(RequestStreetModesMapper.mapRequestStreetModes(modesInput));
    }
    if (viaSegment.containsKey("filters")) {
      journey.withTransit(tb -> {
        List<Map<String, ?>> filters = (List<Map<String, ?>>) viaSegment.get("filters");
        tb.setFilters(transitFilterNewWayMapper.mapFilter(filters));
      });
    }
    return journey.build();
  }
}
