package org.opentripplanner.ext.carpooling.routing;

import org.opentripplanner.framework.geometry.WgsCoordinate;

/**
 * Represents a point along a carpool route.
 * <p>
 * Route points include the boarding area, intermediate stops, and alighting area.
 * Each point has a coordinate and a descriptive label for debugging.
 */
public record RoutePoint(WgsCoordinate coordinate, String label) {
  public RoutePoint {
    if (coordinate == null) {
      throw new IllegalArgumentException("Coordinate cannot be null");
    }
    if (label == null || label.isBlank()) {
      throw new IllegalArgumentException("Label cannot be null or blank");
    }
  }

  @Override
  public String toString() {
    return String.format("%s (%.4f, %.4f)", label, coordinate.latitude(), coordinate.longitude());
  }
}
