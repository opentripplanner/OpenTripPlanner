package org.opentripplanner.place;

import java.util.Collection;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;
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
    StreetMode streetMode,
    boolean reverseDirection
  );

  /**
   * Search closest stops from a given coordinate, extending up to a specified max radius.
   *
   * @param coordinate   Origin
   * @param radiusMeters Search radius from the origin in meters
   */
  List<NearbyStop> findNearbyStops(Coordinate coordinate, double radiusMeters);
}
