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
 * passengers are going in generally the same direction. Uses optimized segment-based
 * analysis to handle routes that take detours (e.g., driving around a lake).
 * <p>
 * <strong>Performance Optimization:</strong> Only checks individual segments and the
 * full route (O(n) complexity) rather than all possible segment ranges (O(n²)).
 * This is sufficient for filtering while maintaining accuracy.
 */
public class DirectionalCompatibilityFilter implements TripFilter {

  private static final Logger LOG = LoggerFactory.getLogger(DirectionalCompatibilityFilter.class);

  /**
   * Default maximum bearing difference for compatibility.
   * 60° allows for reasonable detours while preventing perpendicular or opposite directions.
   */
  public static final double DEFAULT_BEARING_TOLERANCE_DEGREES = 60.0;

  private final double bearingToleranceDegrees;
  private final double corridorToleranceDegrees;

  public DirectionalCompatibilityFilter() {
    this(DEFAULT_BEARING_TOLERANCE_DEGREES, RouteGeometry.DEFAULT_CORRIDOR_TOLERANCE_DEGREES);
  }

  public DirectionalCompatibilityFilter(double bearingToleranceDegrees) {
    this(bearingToleranceDegrees, RouteGeometry.DEFAULT_CORRIDOR_TOLERANCE_DEGREES);
  }

  /**
   * Creates a filter with custom bearing and corridor tolerances.
   *
   * @param bearingToleranceDegrees Maximum bearing difference (in degrees)
   * @param corridorToleranceDegrees Maximum distance from route corridor (in degrees, ~1° = 111km)
   */
  public DirectionalCompatibilityFilter(
    double bearingToleranceDegrees,
    double corridorToleranceDegrees
  ) {
    this.bearingToleranceDegrees = bearingToleranceDegrees;
    this.corridorToleranceDegrees = corridorToleranceDegrees;
  }

  @Override
  public boolean accepts(
    CarpoolTrip trip,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff
  ) {
    // Build route points list
    List<WgsCoordinate> routePoints = buildRoutePoints(trip);

    if (routePoints.size() < 2) {
      LOG.warn("Trip {} has fewer than 2 route points, rejecting", trip.getId());
      return false;
    }

    // Calculate passenger journey bearing
    double passengerBearing = DirectionalCalculator.calculateBearing(
      passengerPickup,
      passengerDropoff
    );

    // OPTIMIZATION: Instead of checking all O(n²) segment ranges,
    // only check:
    // 1. Individual segments (O(n)) - catches most compatible trips
    // 2. Full route - handles end-to-end compatibility

    // Check individual segments
    for (int i = 0; i < routePoints.size() - 1; i++) {
      if (
        isSegmentCompatible(
          routePoints.get(i),
          routePoints.get(i + 1),
          passengerBearing,
          passengerPickup,
          passengerDropoff,
          i,
          i + 1,
          routePoints
        )
      ) {
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

    // Check full route as fallback (handles complex multi-segment compatibility)
    if (
      isSegmentCompatible(
        routePoints.get(0),
        routePoints.get(routePoints.size() - 1),
        passengerBearing,
        passengerPickup,
        passengerDropoff,
        0,
        routePoints.size() - 1,
        routePoints
      )
    ) {
      LOG.debug(
        "Trip {} accepted: passenger journey aligns with full route ({} to {})",
        trip.getId(),
        routePoints.get(0),
        routePoints.get(routePoints.size() - 1)
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
   * Checks if a segment is compatible with the passenger journey.
   *
   * @param segmentStart Start coordinate of the segment
   * @param segmentEnd End coordinate of the segment
   * @param passengerBearing Bearing of passenger journey
   * @param passengerPickup Passenger pickup location
   * @param passengerDropoff Passenger dropoff location
   * @param startIdx Start index in route points (for corridor calculation)
   * @param endIdx End index in route points (for corridor calculation)
   * @param allRoutePoints All route points (for corridor calculation)
   * @return true if segment is directionally compatible and within corridor
   */
  private boolean isSegmentCompatible(
    WgsCoordinate segmentStart,
    WgsCoordinate segmentEnd,
    double passengerBearing,
    WgsCoordinate passengerPickup,
    WgsCoordinate passengerDropoff,
    int startIdx,
    int endIdx,
    List<WgsCoordinate> allRoutePoints
  ) {
    // Calculate segment bearing
    double segmentBearing = DirectionalCalculator.calculateBearing(segmentStart, segmentEnd);

    // Check directional compatibility
    double bearingDiff = DirectionalCalculator.bearingDifference(segmentBearing, passengerBearing);

    if (bearingDiff <= bearingToleranceDegrees) {
      // Also verify that pickup/dropoff are within the route corridor
      List<WgsCoordinate> segmentPoints = allRoutePoints.subList(startIdx, endIdx + 1);
      return RouteGeometry.areBothWithinCorridor(
        segmentPoints,
        passengerPickup,
        passengerDropoff,
        corridorToleranceDegrees
      );
    }

    return false;
  }

  /**
   * Gets the configured bearing tolerance in degrees.
   */
  public double getBearingToleranceDegrees() {
    return bearingToleranceDegrees;
  }

  /**
   * Gets the configured corridor tolerance in degrees.
   */
  public double getCorridorToleranceDegrees() {
    return corridorToleranceDegrees;
  }
}
