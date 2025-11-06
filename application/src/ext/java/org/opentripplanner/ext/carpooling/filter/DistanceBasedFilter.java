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

      double pickupDistanceToSegment = SphericalDistanceLibrary.fastDistance(
        passengerPickup.asJtsCoordinate(),
        segmentStart.asJtsCoordinate(),
        segmentEnd.asJtsCoordinate()
      );
      double dropoffDistanceToSegment = SphericalDistanceLibrary.fastDistance(
        passengerDropoff.asJtsCoordinate(),
        segmentStart.asJtsCoordinate(),
        segmentEnd.asJtsCoordinate()
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

  double getMaxDistanceMeters() {
    return maxDistanceMeters;
  }
}
