package org.opentripplanner.apis.transmodel.mapping;

import java.util.List;
import java.util.Map;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.JourneyRequest;

public class ViaSegmentMapper {

  static JourneyRequest mapViaSegment(RouteRequest defaultRequest, Map<String, Object> viaSegment) {
    JourneyRequest journey = defaultRequest.journey().clone();
    if (viaSegment.containsKey("modes")) {
      Map<String, Object> modesInput = (Map<String, Object>) viaSegment.get("modes");
      journey.setModes(RequestModesMapper.mapRequestModes(modesInput));
    }
    if (viaSegment.containsKey("filters")) {
      List<Map<String, ?>> filters = (List<Map<String, ?>>) viaSegment.get("filters");
      journey.transit().setFilters(FilterMapper.mapFilterNewWay(filters));
    }
    return journey;
  }
}
