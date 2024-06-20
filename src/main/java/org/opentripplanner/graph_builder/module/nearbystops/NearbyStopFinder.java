package org.opentripplanner.graph_builder.module.nearbystops;

import java.util.Collection;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Interface for finding nearby stops from a given vertex. It is used by access
 * and egress searches, and in transfer generation.
 */
public interface NearbyStopFinder {
  /**
   * Return all stops within a certain distance from the given vertex.
   */
  Collection<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetRequest streetRequest,
    boolean reverseDirection
  );
}
