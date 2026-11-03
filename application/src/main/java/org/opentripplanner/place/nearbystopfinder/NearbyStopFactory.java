package org.opentripplanner.place.nearbystopfinder;

import static org.opentripplanner.place.api.NearbyStop.ofZeroDistance;

import java.util.List;
import java.util.Set;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;

public class NearbyStopFactory {

  /**
   * Create zero distance NearbyStops given a list of TransitStopVertices
   */
  public static List<NearbyStop> nearbyStopsForTransitStopVertices(
    Set<TransitStopVertex> stopVertices,
    boolean reverseDirection,
    RouteRequest routeRequest,
    StreetMode mode
  ) {
    if (stopVertices.isEmpty()) {
      return List.of();
    }

    var streetSearchRequest = StreetSearchRequestMapper.mapToTransferRequest(routeRequest)
      .withArriveBy(reverseDirection)
      .withMode(mode)
      .build();

    return stopVertices
      .stream()
      .map(s -> ofZeroDistance(s.getId(), new State(s, streetSearchRequest)))
      .toList();
  }
}
