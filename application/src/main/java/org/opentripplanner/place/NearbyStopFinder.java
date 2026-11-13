package org.opentripplanner.place;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Interface for finding nearby stops from a given vertex. It is used by access and egress searches,
 * and in transfer generation.
 */
public interface NearbyStopFinder {
  /**
   * Return all stops within a certain distance from the given vertex.
   *
   * @param maxStopCount The maximum stops to return. 0 means no limit.
   */
  Collection<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetMode streetMode,
    boolean reverseDirection,
    Duration durationLimit,
    int maxStopCount
  );

  /**
   * Same as {@link #findNearbyStops(Vertex, RouteRequest, StreetMode, boolean, Duration, int)},
   * but the paths found do not use the elevators with an id in {@code avoidElevatorIds}.
   *
   * @throws UnsupportedOperationException if the finder can not avoid elevators and the set is not
   *                                       empty
   */
  default Collection<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetMode streetMode,
    boolean reverseDirection,
    Duration durationLimit,
    int maxStopCount,
    Set<String> avoidElevatorIds
  ) {
    if (!avoidElevatorIds.isEmpty()) {
      throw new UnsupportedOperationException("This finder can not avoid elevators.");
    }
    return findNearbyStops(
      vertex,
      routingRequest,
      streetMode,
      reverseDirection,
      durationLimit,
      maxStopCount
    );
  }

  /**
   * Search closest stops from a given coordinate, extending up to a specified max radius.
   *
   * @param coordinate   Origin
   * @param radiusMeters Search radius from the origin in meters
   */
  List<NearbyStop> findNearbyStops(Coordinate coordinate, double radiusMeters);
}
