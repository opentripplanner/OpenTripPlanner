package org.opentripplanner.ext.carpooling.routing;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;

/**
 * The region in which a detour of a driver leg can still be feasible.
 * <p>
 * A leg from {@code a} to {@code b} that may take at most {@code L} seconds once a passenger is
 * inserted can only pass through vertices {@code v} with {@code t(a→v) + t(v→b) ≤ L}. No street
 * route is faster than the straight line driven at the fastest speed in the graph, so
 * {@code beeline(v, b) / maxCarSpeed} is a lower bound on {@code t(v→b)}, and a vertex whose
 * elapsed time plus that bound exceeds {@code L} cannot lie on a feasible detour. Geometrically an
 * ellipse with foci {@code a} and {@code b}, where a plain duration limit would explore a disc of
 * radius {@code L}. For a reverse tree rooted at {@code b} the focus is {@code a}.
 */
final class EllipseBounds {

  private final double focusLat;
  private final double focusLon;
  private final long boundSeconds;
  private final double maxCarSpeedMetersPerSecond;

  /**
   * @param focus the far end of the leg the tree serves
   * @param boundSeconds the most seconds the whole leg may take
   * @param maxCarSpeedMetersPerSecond the fastest speed any street in the graph can be driven at;
   *        a smaller value would prune feasible detours on fast roads
   */
  EllipseBounds(Coordinate focus, long boundSeconds, double maxCarSpeedMetersPerSecond) {
    if (maxCarSpeedMetersPerSecond <= 0) {
      throw new IllegalArgumentException("maxCarSpeed must be positive");
    }
    this.focusLat = focus.y;
    this.focusLon = focus.x;
    this.boundSeconds = boundSeconds;
    this.maxCarSpeedMetersPerSecond = maxCarSpeedMetersPerSecond;
  }

  /** Whether a vertex at {@code (lat, lon)}, reached after {@code elapsedSeconds}, can still be on a feasible detour. */
  boolean contains(long elapsedSeconds, double lat, double lon) {
    // fastDistance may over-estimate by a fraction of a percent; MAX_ERR_INV keeps it a lower bound.
    double meters =
      SphericalDistanceLibrary.fastDistance(lat, lon, focusLat, focusLon) *
      SphericalDistanceLibrary.MAX_ERR_INV;
    return elapsedSeconds + meters / maxCarSpeedMetersPerSecond <= boundSeconds;
  }
}
