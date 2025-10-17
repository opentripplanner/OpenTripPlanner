package org.opentripplanner.ext.carpooling.filter;

import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters trips based on geographic proximity to the passenger journey.
 * <p>
 * Checks if the passenger's pickup and dropoff locations are both within
 * a reasonable distance from the driver's route. The filter considers all
 * segments of the driver's route (including intermediate stops), allowing
 * passengers to join trips where they share a segment of the driver's journey,
 * while rejecting passengers whose journey is far off any part of the driver's path.
 */
public class DistanceBasedFilter implements TripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(DistanceBasedFilter.class);

  /**
   * Default maximum distance: 50km.
   * If all segments of the trip's route are more than this distance from
   * both passenger pickup and dropoff, the trip is rejected.
   */
  public static final double DEFAULT_MAX_DISTANCE_METERS = 50_000;

  private final double maxDistanceMeters;

  public DistanceBasedFilter() {
    this(DEFAULT_MAX_DISTANCE_METERS);
  }

  public DistanceBasedFilter(double maxDistanceMeters) {
    this.maxDistanceMeters = maxDistanceMeters;
  }

  @Override
  public boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    List<WgsCoordinate> routePoints = trip.routePoints();

    if (routePoints.size() < 2) {
      LOG.warn("Trip {} has fewer than 2 route points, rejecting", trip.getId());
      return false;
    }

    // Check each segment of the route
    for (int i = 0; i < routePoints.size() - 1; i++) {
      WgsCoordinate segmentStart = routePoints.get(i);
      WgsCoordinate segmentEnd = routePoints.get(i + 1);

      double pickupDistanceToSegment = distanceToLineSegment(
        passengerPickup,
        segmentStart,
        segmentEnd
      );
      double dropoffDistanceToSegment = distanceToLineSegment(
        passengerDropoff,
        segmentStart,
        segmentEnd
      );

      // Accept if either passenger location is within threshold of this segment
      if (
        pickupDistanceToSegment <= maxDistanceMeters ||
        dropoffDistanceToSegment <= maxDistanceMeters
      ) {
        LOG.debug(
          "Trip {} accepted by distance filter: passenger journey close to segment {} ({} to {}). " +
          "Pickup distance: {:.0f}m, Dropoff distance: {:.0f}m (max: {:.0f}m)",
          trip.getId(),
          i,
          segmentStart,
          segmentEnd,
          pickupDistanceToSegment,
          dropoffDistanceToSegment,
          maxDistanceMeters
        );
        return true;
      }
    }

    LOG.debug(
      "Trip {} rejected by distance filter: passenger journey too far from all route segments (max: {:.0f}m)",
      trip.getId(),
      maxDistanceMeters
    );
    return false;
  }

  /**
   * Calculates the distance from a point to a line segment.
   * <p>
   * This finds the closest point on the line segment from lineStart to lineEnd,
   * then calculates the spherical distance from the point to that closest point.
   * <p>
   * The algorithm:
   * 1. Projects the point onto the infinite line passing through lineStart and lineEnd
   * 2. Clamps the projection to stay within the segment [lineStart, lineEnd]
   * 3. Calculates the spherical distance from the point to the closest point on the segment
   * <p>
   * Note: Uses lat/lon as if they were Cartesian coordinates for the projection
   * calculation, which is an approximation. For typical carpooling distances
   * (urban/suburban scale), this approximation is acceptable.
   *
   * @param point The point to measure from
   * @param lineStart Start of the line segment
   * @param lineEnd End of the line segment
   * @return Distance in meters from point to the closest point on the line segment
   */
  private double distanceToLineSegment(
    WgsCoordinate point,
    WgsCoordinate lineStart,
    WgsCoordinate lineEnd
  ) {
    // If start and end are the same point, return distance to that point
    if (lineStart.equals(lineEnd)) {
      return SphericalDistanceLibrary.fastDistance(
        point.asJtsCoordinate(),
        lineStart.asJtsCoordinate()
      );
    }

    // Calculate vector from lineStart to lineEnd
    double dx = lineEnd.longitude() - lineStart.longitude();
    double dy = lineEnd.latitude() - lineStart.latitude();

    // Calculate squared length of line segment
    double lineLengthSquared = dx * dx + dy * dy;

    // Calculate projection parameter t
    // t represents where the projection falls on the line segment:
    // t = 0 means the projection is at lineStart
    // t = 1 means the projection is at lineEnd
    // t between 0 and 1 means the projection is between them
    double t =
      ((point.longitude() - lineStart.longitude()) * dx +
        (point.latitude() - lineStart.latitude()) * dy) /
      lineLengthSquared;

    // Clamp t to [0, 1] to ensure we stay on the segment
    t = Math.max(0, Math.min(1, t));

    // Calculate the closest point on the segment
    double closestLon = lineStart.longitude() + t * dx;
    double closestLat = lineStart.latitude() + t * dy;
    WgsCoordinate closestPoint = new WgsCoordinate(closestLat, closestLon);

    // Return spherical distance from point to closest point on segment
    return SphericalDistanceLibrary.fastDistance(
      point.asJtsCoordinate(),
      closestPoint.asJtsCoordinate()
    );
  }

  /**
   * Gets the configured maximum distance in meters.
   */
  public double getMaxDistanceMeters() {
    return maxDistanceMeters;
  }
}
