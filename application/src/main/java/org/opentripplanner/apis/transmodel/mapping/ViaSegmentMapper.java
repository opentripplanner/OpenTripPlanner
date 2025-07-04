package org.opentripplanner.apis.transmodel.mapping;

import java.util.List;
import java.util.Map;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.JourneyRequest;

class ViaSegmentMapper {

  static JourneyRequest mapViaSegment(RouteRequest defaultRequest, Map<String, Object> viaSegment) {
    var journey = defaultRequest.journey().copyOf();
    if (viaSegment.containsKey("modes")) {
      Map<String, Object> modesInput = (Map<String, Object>) viaSegment.get("modes");
      journey.setModes(RequestStreetModesMapper.mapRequestStreetModes(modesInput));
    }
    if (viaSegment.containsKey("filters")) {
      journey.withTransit(tb -> {
        List<Map<String, ?>> filters = (List<Map<String, ?>>) viaSegment.get("filters");
        tb.setFilters(TransitFilterNewWayMapper.mapFilter(filters));
      });
    }
    return journey.build();
  }
}
