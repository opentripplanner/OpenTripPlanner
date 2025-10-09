package org.opentripplanner.ext.carpooling.util;

import java.util.List;
import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * Utility methods for working with route geometry and geographic relationships.
 */
public class RouteGeometry {

  /** Tolerance for corridor checks (approximately 10km in degrees at mid-latitudes) */
  public static final double DEFAULT_CORRIDOR_TOLERANCE_DEGREES = 0.1;

  /**
   * Represents a geographic bounding box.
   */
  public record BoundingBox(double minLat, double maxLat, double minLon, double maxLon) {
    /**
     * Checks if a coordinate is within this bounding box.
     */
    public boolean contains(WgsCoordinate coord) {
      return (
        coord.latitude() >= minLat &&
        coord.latitude() <= maxLat &&
        coord.longitude() >= minLon &&
        coord.longitude() <= maxLon
      );
    }

    /**
     * Expands this bounding box by the given tolerance.
     */
    public BoundingBox expand(double tolerance) {
      return new BoundingBox(
        minLat - tolerance,
        maxLat + tolerance,
        minLon - tolerance,
        maxLon + tolerance
      );
    }
  }

  /**
   * Calculates a bounding box for a list of coordinates.
   *
   * @param coordinates List of coordinates
   * @return Bounding box containing all coordinates
   */
  public static BoundingBox calculateBoundingBox(List<WgsCoordinate> coordinates) {
    if (coordinates.isEmpty()) {
      throw new IllegalArgumentException("Cannot calculate bounding box for empty list");
    }

    double minLat = Double.MAX_VALUE;
    double maxLat = -Double.MAX_VALUE;
    double minLon = Double.MAX_VALUE;
    double maxLon = -Double.MAX_VALUE;

    for (WgsCoordinate coord : coordinates) {
      minLat = Math.min(minLat, coord.latitude());
      maxLat = Math.max(maxLat, coord.latitude());
      minLon = Math.min(minLon, coord.longitude());
      maxLon = Math.max(maxLon, coord.longitude());
    }

    return new BoundingBox(minLat, maxLat, minLon, maxLon);
  }

  /**
   * Checks if a coordinate is within a corridor defined by a route segment.
   * <p>
   * This prevents matching passengers who are directionally aligned but geographically
   * far from the actual route (e.g., parallel roads on opposite sides of a city).
   *
   * @param routeSegment Coordinates defining the route corridor
   * @param coordinate Coordinate to check
   * @param toleranceDegrees Corridor width tolerance in degrees
   * @return true if the coordinate is within the corridor
   */
  public static boolean isWithinCorridor(
    List<WgsCoordinate> routeSegment,
    WgsCoordinate coordinate,
    double toleranceDegrees
  ) {
    BoundingBox box = calculateBoundingBox(routeSegment);
    BoundingBox expandedBox = box.expand(toleranceDegrees);
    return expandedBox.contains(coordinate);
  }

  /**
   * Checks if a coordinate is within a corridor using default tolerance.
   */
  public static boolean isWithinCorridor(
    List<WgsCoordinate> routeSegment,
    WgsCoordinate coordinate
  ) {
    return isWithinCorridor(routeSegment, coordinate, DEFAULT_CORRIDOR_TOLERANCE_DEGREES);
  }

  /**
   * Checks if both pickup and dropoff are within the route corridor.
   */
  public static boolean areBothWithinCorridor(
    List<WgsCoordinate> routeSegment,
    WgsCoordinate pickup,
    WgsCoordinate dropoff,
    double toleranceDegrees
  ) {
    BoundingBox box = calculateBoundingBox(routeSegment);
    BoundingBox expandedBox = box.expand(toleranceDegrees);
    return expandedBox.contains(pickup) && expandedBox.contains(dropoff);
  }

  /**
   * Checks if both coordinates are within the corridor using default tolerance.
   */
  public static boolean areBothWithinCorridor(
    List<WgsCoordinate> routeSegment,
    WgsCoordinate pickup,
    WgsCoordinate dropoff
  ) {
    return areBothWithinCorridor(routeSegment, pickup, dropoff, DEFAULT_CORRIDOR_TOLERANCE_DEGREES);
  }
}
