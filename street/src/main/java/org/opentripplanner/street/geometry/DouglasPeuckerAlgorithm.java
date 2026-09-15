package org.opentripplanner.street.geometry;

import java.util.ArrayDeque;
import java.util.BitSet;
import java.util.Deque;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Simplifies a {@link LineString} with the Douglas-Peucker algorithm (also known as Ramer-Douglas-Peucker, or RDP),
 * operating on geographic (WGS84 longitude/latitude, in degrees) coordinates.
 *
 * <p>Away from the equator, a degree of longitude covers fewer real-world meters than a degree of latitude, so the
 * distance calculation scales longitude by {@code cos(latitude)} to correct for that. This is the standard
 * <a href="https://en.wikipedia.org/wiki/Equirectangular_projection"> equirectangular-projection</a> approximation,
 * which treats the Earth as a sphere; it uses the line's approximate average latitude
 * (see {@link #approximateAverageLatitude}), not the latitude of each individual point being measured. We copute
 * the longitude scale once - this is a perfomance optimization. The scaling is applied only inside the distance
 * calculation, never on the coordinates themselves. That means every retained point, endpoints included, is the exact
 * original {@link Coordinate} instance: there is no scale/unscale round-trip, and so no rounding error that could move
 * a point off its original location.
 *
 * <p>JTS ships its own Douglas-Peucker simplifier, but it treats coordinates as planar (Cartesian) and does not apply
 * the longitude correction above, so it distorts distances when applied directly to WGS84 coordinates. This
 * implementation exists to support those coordinates correctly.
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
      throw new IllegalArgumentException("toleranceMeters must not be negative");
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
   * Keeps the point in each {@code [start, end]} range furthest from the chord between its
   * endpoints if it exceeds {@code toleranceDegrees}, then splits into two halves and keeps
   * going. The result is stored in the {@link #keep} BitSet.
   *
   * <p>This is iterative, with the pending ranges on an explicit heap-allocated {@code stack},
   * rather than recursive: a naive recursive version can recurse as deep as the input is long
   * (e.g. a shape whose furthest point from the chord always lands near one end), which risks a
   * {@link StackOverflowError} on the JVM call stack for long, detailed shapes.
   */
  private void douglasPeucker(int start, int end) {
    Deque<Range> stack = new ArrayDeque<>();
    stack.push(new Range(start, end));

    while (!stack.isEmpty()) {
      var r = stack.pop();

      if (r.start() + 1 == r.end()) {
        continue;
      }

      double maxDistance = -1;
      int maxIndex = -1;

      for (int i = r.start() + 1; i < r.end(); i++) {
        double distance = perpendicularDistance(
          coordinates[r.start()],
          coordinates[r.end()],
          coordinates[i],
          lonScale
        );
        if (distance > maxDistance) {
          maxDistance = distance;
          maxIndex = i;
        }
      }
      if (maxDistance > toleranceDegrees) {
        keep.set(maxIndex);
        stack.push(new Range(r.start(), maxIndex));
        stack.push(new Range(maxIndex, r.end()));
      }
    }
  }

  /**
   * Distance from {@code point} to the segment between {@code start} and {@code end}, in degrees, with longitude
   * scaled by {@link #lonScale}. Using the segment rather than the infinite line through it matters for a point that
   * overshoots past {@code start} or {@code end} while staying close to that line's bearing: measured against the
   * infinite line such a point looks almost colinear (near-zero distance) even though reaching it is a real,
   * arbitrarily long detour, which would wrongly let it be simplified away.
   */
  static double perpendicularDistance(
    Coordinate start,
    Coordinate end,
    Coordinate point,
    double lonScale
  ) {
    double x1 = start.x * lonScale;
    double y1 = start.y;
    double x2 = end.x * lonScale;
    double y2 = end.y;
    double px = point.x * lonScale;
    double py = point.y;

    double dx = x2 - x1;
    double dy = y2 - y1;
    double xySquared = dx * dx + dy * dy;

    if (xySquared == 0) {
      return Math.hypot(px - x1, py - y1);
    }

    double t = Math.clamp(((px - x1) * dx + (py - y1) * dy) / xySquared, 0.0, 1.0);
    return Math.hypot(px - (x1 + t * dx), py - (y1 + t * dy));
  }

  /**
   * Approximates the line's average latitude from its first, middle, and last point.
   */
  private static double approximateAverageLatitude(Coordinate[] coordinates) {
    int lastIndex = coordinates.length - 1;
    return (coordinates[0].y + coordinates[lastIndex / 2].y + coordinates[lastIndex].y) / 3;
  }

  private record Range(int start, int end) {}
}
