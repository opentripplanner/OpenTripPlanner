package org.opentripplanner.graph_builder.module.nearbystops;

import java.util.Collection;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;

/**
 * Interface for finding nearby stops from a given vertex. It is used by access
 * and egress searches, and in transfer generation.
 */
public interface NearbyStopFinder {
  /**
   * Return all stops within a certain distance from the given vertex.
   */
  Collection<NearbyStop> findNearbyStops(Vertex vertex, StreetSearchRequest request);
}
