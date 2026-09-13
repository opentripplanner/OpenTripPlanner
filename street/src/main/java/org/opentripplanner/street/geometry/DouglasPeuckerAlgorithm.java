package org.opentripplanner.street.geometry;

import java.util.BitSet;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Simplifies a {@link LineString} with the Douglas-Peucker algorithm. A degree of longitude
 * covers fewer real-world meters than a degree of latitude away from the equator, so the
 * distance calculation scales longitude by {@code cos(latitude)} to correct for that - but only
 * inside the distance calculation, never on the coordinates themselves. That means every
 * retained point, endpoints included, is the exact original {@link Coordinate} instance: there is
 * no scale/unscale round-trip, and so no rounding error that could move a point off its original
 * location.
 */
public class DouglasPeuckerAlgorithm {

  private final Coordinate[] coordinates;
  private final double toleranceDegrees;
  private final double lonScale;
  private final BitSet keep;

  private DouglasPeuckerAlgorithm(Coordinate[] coordinates, double toleranceMeters) {
    this.coordinates = coordinates;
    this.toleranceDegrees = SphericalDistanceLibrary.metersToDegrees(toleranceMeters);
    this.lonScale = Math.cos(Math.toRadians(approximateAverageLatitude(coordinates)));
    this.keep = new BitSet(coordinates.length);
    this.keep.set(0);
    this.keep.set(coordinates.length - 1);
  }

  /**
   * Simplifies {@code lineString} to within {@code toleranceMeters}, keeping its first and last
   * point exactly. A {@code toleranceMeters} of {@code 0} disables simplification.
   *
   * @throws IllegalArgumentException if {@code toleranceMeters} is negative.
   */
  public static LineString of(LineString lineString, double toleranceMeters) {
    if (toleranceMeters < 0.0) {
      throw new IllegalArgumentException("toleranceMeters must be greater than 0");
    }
    if (toleranceMeters == 0.0 || lineString.getNumPoints() < 3) {
      return lineString;
    }
    var simplified = new DouglasPeuckerAlgorithm(
      lineString.getCoordinates(),
      toleranceMeters
    ).simplify();
    return simplified.length == lineString.getNumPoints()
      ? lineString
      : GeometryUtils.makeLineString(simplified);
  }

  /**
   * Runs the algorithm and returns the simplified coordinates.
   */
  public Coordinate[] simplify() {
    douglasPeucker(0, coordinates.length - 1);

    int kept = keep.cardinality();
    if (kept == coordinates.length) {
      return coordinates;
    }
    Coordinate[] result = new Coordinate[kept];
    int j = 0;
    for (int i = 0; i < coordinates.length; i++) {
      if (keep.get(i)) {
        result[j] = coordinates[i];
        ++j;
      }
    }
    return result;
  }

  /**
   * Keeps the point in {@code [start, end]} furthest from the chord between its endpoints if it
   * exceeds {@code toleranceDegrees}, then recurses on both halves.
   */
  private void douglasPeucker(int start, int end) {
    if (end <= start + 1) {
      return;
    }
    double maxDistance = -1;
    int maxIndex = -1;
    for (int i = start + 1; i < end; i++) {
      double distance = perpendicularDistance(coordinates[start], coordinates[end], coordinates[i]);
      if (distance > maxDistance) {
        maxDistance = distance;
        maxIndex = i;
      }
    }
    if (maxDistance > toleranceDegrees) {
      keep.set(maxIndex);
      douglasPeucker(start, maxIndex);
      douglasPeucker(maxIndex, end);
    }
  }

  /**
   * Distance from {@code point} to the segment between {@code start} and {@code end}, in
   * degrees, with longitude scaled by {@link #lonScale}. Using the segment rather than the
   * infinite line through it matters for a point that overshoots past {@code start} or
   * {@code end} while staying close to that line's bearing: measured against the infinite line
   * such a point looks almost colinear (near-zero distance) even though reaching it is a real,
   * arbitrarily long detour, which would wrongly let it be simplified away.
   */
  private double perpendicularDistance(Coordinate start, Coordinate end, Coordinate point) {
    double x1 = start.x * lonScale;
    double y1 = start.y;
    double x2 = end.x * lonScale;
    double y2 = end.y;
    double px = point.x * lonScale;
    double py = point.y;

    double dx = x2 - x1;
    double dy = y2 - y1;
    double lengthSquared = dx * dx + dy * dy;
    if (lengthSquared == 0) {
      return Math.hypot(px - x1, py - y1);
    }

    double t = Math.clamp(((px - x1) * dx + (py - y1) * dy) / lengthSquared, 0.0, 1.0);
    return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
  }

  /**
   * Approximates the line's average latitude from its first, middle and last point.
   */
  private static double approximateAverageLatitude(Coordinate[] coordinates) {
    int lastIndex = coordinates.length - 1;
    return (coordinates[0].y + coordinates[lastIndex / 2].y + coordinates[lastIndex].y) / 3;
  }
}
