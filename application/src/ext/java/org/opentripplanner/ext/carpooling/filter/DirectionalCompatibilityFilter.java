package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.street.geometry.DirectionUtils;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters trips based on directional compatibility with the passenger journey.
 * <p>
 * This prevents carpooling from becoming a taxi service by ensuring trips and
 * passengers are going in generally the same direction. Uses optimized segment-based
 * analysis to handle routes that take detours (e.g., driving around a lake).
 * <p>
 */
public class DirectionalCompatibilityFilter implements TripFilter, AccessEgressTripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(DirectionalCompatibilityFilter.class);

  /**
   * Default maximum bearing difference for compatibility.
   * 60° allows for reasonable detours while preventing perpendicular or opposite directions.
   */
  public static final double DEFAULT_BEARING_TOLERANCE_DEGREES = 60.0;

  private final double bearingToleranceDegrees;

  public DirectionalCompatibilityFilter() {
    this(DEFAULT_BEARING_TOLERANCE_DEGREES);
  }

  public DirectionalCompatibilityFilter(double bearingToleranceDegrees) {
    this.bearingToleranceDegrees = bearingToleranceDegrees;
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

    double passengerBearing = DirectionUtils.getAzimuth(
      passengerPickup.asJtsCoordinate(),
      passengerDropoff.asJtsCoordinate()
    );

    for (int i = 0; i < routePoints.size() - 1; i++) {
      if (isSegmentCompatible(routePoints.get(i), routePoints.get(i + 1), passengerBearing)) {
        LOG.debug(
          "Trip {} accepted: passenger journey aligns with segment {} ({} to {})",
          trip.getId(),
          i,
          routePoints.get(i),
          routePoints.get(i + 1)
        );
        return true;
      }
    }

    // Check full route as fallback
    if (isSegmentCompatible(routePoints.getFirst(), routePoints.getLast(), passengerBearing)) {
      LOG.debug(
        "Trip {} accepted: passenger journey aligns with full route ({} to {})",
        trip.getId(),
        routePoints.getFirst(),
        routePoints.getLast()
      );
      return true;
    }

    LOG.debug(
      "Trip {} rejected by directional filter: passenger journey (bearing {}°) not aligned with any route segments",
      trip.getId(),
      Math.round(passengerBearing)
    );
    return false;
  }

  @Override
  public boolean acceptsAccessEgress(
    CarpoolTrip trip,
    WgsCoordinate coordinateOfPassenger,
    Instant passengerDepartureTime,
    Duration searchWindow
  ) {
    var tripStartCoordinate = trip.routePoints().getFirst().asJtsCoordinate();
    var tripEndCoordinate = trip.routePoints().getLast().asJtsCoordinate();
    var passengerCoordJts = coordinateOfPassenger.asJtsCoordinate();

    var tripBearing = DirectionUtils.getAzimuth(tripStartCoordinate, tripEndCoordinate);
    var startToPassengerBearing = DirectionUtils.getAzimuth(tripStartCoordinate, passengerCoordJts);
    var endToPassengerBearing = DirectionUtils.getAzimuth(passengerCoordJts, tripEndCoordinate);

    return (
      bearingsAreWithinTolerance(tripBearing, startToPassengerBearing) &&
      bearingsAreWithinTolerance(tripBearing, endToPassengerBearing)
    );
  }

  double getBearingToleranceDegrees() {
    return bearingToleranceDegrees;
  }

  /**
   * Checks if a segment is directionally compatible with the passenger journey.
   *
   * @param segmentStart Start coordinate of the segment
   * @param segmentEnd End coordinate of the segment
   * @param passengerBearing Bearing of passenger journey
   * @return true if segment bearing is within tolerance of passenger bearing
   */
  private boolean isSegmentCompatible(
    WgsCoordinate segmentStart,
    WgsCoordinate segmentEnd,
    double passengerBearing
  ) {
    double segmentBearing = DirectionUtils.getAzimuth(
      segmentStart.asJtsCoordinate(),
      segmentEnd.asJtsCoordinate()
    );

    return bearingsAreWithinTolerance(segmentBearing, passengerBearing);
  }

  private boolean bearingsAreWithinTolerance(double bearing1, double bearing2) {
    double bearingDiff = DirectionUtils.bearingDifference(bearing1, bearing2);
    return bearingDiff <= bearingToleranceDegrees;
  }
}
