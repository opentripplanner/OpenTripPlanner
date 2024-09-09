package org.opentripplanner.graph_builder.module.nearbystops;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;

public class DirectlyConnectedStopFinder {

  /**
   * Given a list of vertices, find the ones that correspond to stops and return them as NearbyStops
   */
  public static List<NearbyStop> findDirectlyConnectedStops(
    Set<Vertex> originVertices,
    boolean reverseDirection,
    RouteRequest request,
    StreetRequest streetRequest
  ) {
    List<NearbyStop> stopsFound = new ArrayList<>();

    StreetSearchRequest streetSearchRequest = StreetSearchRequestMapper
      .mapToTransferRequest(request)
      .withArriveBy(reverseDirection)
      .withMode(streetRequest.mode())
      .build();

    for (Vertex vertex : originVertices) {
      if (vertex instanceof TransitStopVertex tsv) {
        stopsFound.add(
          new NearbyStop(
            tsv.getStop(),
            0,
            Collections.emptyList(),
            new State(vertex, streetSearchRequest)
          )
        );
      }
    }

    return stopsFound;
  }
}
