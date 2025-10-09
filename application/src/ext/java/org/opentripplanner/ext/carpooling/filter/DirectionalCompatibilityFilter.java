package org.opentripplanner.ext.carpooling.filter;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.util.DirectionalCalculator;
import org.opentripplanner.ext.carpooling.util.RouteGeometry;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters trips based on directional compatibility with the passenger journey.
 * <p>
 * This prevents carpooling from becoming a taxi service by ensuring trips and
 * passengers are going in generally the same direction. Uses segment-based
 * analysis to handle routes that take detours (e.g., driving around a lake).
 */
public class DirectionalCompatibilityFilter implements TripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(DirectionalCompatibilityFilter.class);

  /** Default maximum bearing difference for compatibility */
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
    // Build route points list
    List<WgsCoordinate> routePoints = buildRoutePoints(trip);

    // Check if passenger journey is compatible with any segment of the route
    double passengerBearing = DirectionalCalculator.calculateBearing(
      passengerPickup,
      passengerDropoff
    );

    // Try all possible segment ranges
    for (int startIdx = 0; startIdx < routePoints.size() - 1; startIdx++) {
      for (int endIdx = startIdx + 1; endIdx < routePoints.size(); endIdx++) {
        if (
          isSegmentRangeCompatible(
            routePoints,
            startIdx,
            endIdx,
            passengerBearing,
            passengerPickup,
            passengerDropoff
          )
        ) {
          LOG.debug(
            "Trip {} accepted: passenger journey aligns with route segments {} to {}",
            trip.getId(),
            startIdx,
            endIdx
          );
          return true;
        }
      }
    }

    LOG.debug(
      "Trip {} rejected by directional filter: passenger journey (bearing {}°) not aligned with any route segments",
      trip.getId(),
      Math.round(passengerBearing)
    );
    return false;
  }

  /**
   * Builds the list of route points (boarding → stops → alighting).
   */
  private List<WgsCoordinate> buildRoutePoints(CarpoolTrip trip) {
    List<WgsCoordinate> points = new ArrayList<>();

    // Add boarding area
    points.add(trip.boardingArea().getCoordinate());

    // Add existing stops
    for (CarpoolStop stop : trip.stops()) {
      points.add(stop.getCoordinate());
    }

    // Add alighting area
    points.add(trip.alightingArea().getCoordinate());

    return points;
  }

  /**
   * Checks if a range of route segments is compatible with the passenger journey.
   */
  private boolean isSegmentRangeCompatible(
    List<WgsCoordinate> routePoints,
    int startIdx,
    int endIdx,
    double passengerBearing,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    // Calculate overall bearing for this segment range
    WgsCoordinate rangeStart = routePoints.get(startIdx);
    WgsCoordinate rangeEnd = routePoints.get(endIdx);
    double rangeBearing = DirectionalCalculator.calculateBearing(rangeStart, rangeEnd);

    // Check directional compatibility
    double bearingDiff = DirectionalCalculator.bearingDifference(rangeBearing, passengerBearing);

    if (bearingDiff <= bearingToleranceDegrees) {
      // Also verify that pickup/dropoff are within the route corridor
      List<WgsCoordinate> segmentPoints = routePoints.subList(startIdx, endIdx + 1);
      return RouteGeometry.areBothWithinCorridor(segmentPoints, passengerPickup, passengerDropoff);
    }

    return false;
  }
}
