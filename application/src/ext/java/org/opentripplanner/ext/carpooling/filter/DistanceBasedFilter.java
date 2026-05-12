package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters trips based on geographic proximity to the passenger journey.
 * <p>
 * Checks if at least one of the passenger's pickup and dropoff locations is within a reasonable
 * distance from the driver's route. The filter considers all segments of the driver's route
 * (including intermediate stops), allowing passengers to join trips where they share a segment of
 * the driver's journey, while rejecting passengers whose journey is far off any part of the
 * driver's path.
 */
public class DistanceBasedFilter implements CarpoolTripFilter {

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
  public boolean isCandidateTrip(
    CarpoolTrip trip,
    CarpoolingRequest request,
    Duration searchWindow
  ) {
    return request.isAccessEgressRequest()
      ? isProximateForAccessEgress(trip, request)
      : isProximateForDirect(trip, request);
  }

  private boolean isProximateForDirect(CarpoolTrip trip, CarpoolingRequest request) {
    var passengerPickup = request.getPassengerPickup();
    var passengerDropoff = request.getPassengerDropoff();
    List<WgsCoordinate> routePoints = trip.routePoints();

    if (routePoints.size() < 2) {
      LOG.warn("Trip {} has fewer than 2 route points, rejecting", trip.getId());
      return false;
    }

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

      if (
        pickupDistanceToSegment <= maxDistanceMeters ||
        dropoffDistanceToSegment <= maxDistanceMeters
      ) {
        LOG.debug(
          "Trip {} accepted by distance filter: passenger journey close to segment {} ({} to {}). Pickup distance: {}m, Dropoff distance: {}m (max: {}m)",
          trip.getId(),
          i,
          segmentStart,
          segmentEnd,
          Math.round(pickupDistanceToSegment),
          Math.round(dropoffDistanceToSegment),
          Math.round(maxDistanceMeters)
        );
        return true;
      }
    }

    LOG.debug(
      "Trip {} rejected by distance filter: passenger journey too far from all route segments (max: {}m)",
      trip.getId(),
      Math.round(maxDistanceMeters)
    );
    return false;
  }

  // length of the trip is longer than the length from the trip to the passenger
  private boolean isProximateForAccessEgress(CarpoolTrip trip, CarpoolingRequest request) {
    var tripStart = trip.routePoints().getFirst().asJtsCoordinate();
    var tripEnd = trip.routePoints().getLast().asJtsCoordinate();
    var passengerCoordJts = request.isAccessRequest()
      ? request.getPassengerPickup().asJtsCoordinate()
      : request.getPassengerDropoff().asJtsCoordinate();

    var tripLength = SphericalDistanceLibrary.distance(tripStart, tripEnd);
    var lengthFromTripToPassenger = SphericalDistanceLibrary.fastDistance(
      passengerCoordJts,
      tripStart,
      tripEnd
    );

    return tripLength > lengthFromTripToPassenger;
  }

  double getMaxDistanceMeters() {
    return maxDistanceMeters;
  }
}
