package org.opentripplanner.ext.carpooling.routing;

import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;

/**
 * The region in which a detour of one or more driver legs can still be feasible.
 * <p>
 * A driver leg from {@code a} to {@code b} that may be stretched to at most {@code L} seconds (its
 * duration plus the allowed deviation) can only pass through vertices {@code v} with
 * {@code t(a→v) + t(v→b) ≤ L}. No street route is faster than the straight line driven at the
 * fastest speed in the graph, so {@code beeline(v, b) / maxCarSpeed} is a lower bound on
 * {@code t(v→b)}, and any vertex whose elapsed time plus that bound exceeds {@code L} cannot lie on
 * a feasible detour. Geometrically this is an ellipse with foci {@code a} and {@code b}; a plain
 * duration limit explores a disc of radius {@code L} around {@code a}, most of which no feasible
 * detour ever enters.
 * <p>
 * A tree rooted at {@code a} may serve several legs (co-located waypoints of different trips), so
 * the region is the union of one ellipse per {@link Focus}: a vertex is inside if it is inside any
 * of them. For a reverse (arrive-by) tree rooted at {@code b} the focus is {@code a} and the
 * arithmetic is the same.
 */
final class EllipseBounds {

  /** The far end of a leg served by the tree, and the most seconds the whole leg may take. */
  record Focus(Coordinate coordinate, long boundSeconds) {}

  private final double[] focusLat;
  private final double[] focusLon;
  private final long[] boundSeconds;
  private final double maxCarSpeedMetersPerSecond;

  /**
   * @param foci the legs the tree serves; must not be empty
   * @param maxCarSpeedMetersPerSecond the fastest speed any street in the graph can be driven at.
   *        Dividing the beeline by it keeps the remaining-time estimate a lower bound; a smaller
   *        value would prune feasible detours on fast roads.
   */
  EllipseBounds(List<Focus> foci, double maxCarSpeedMetersPerSecond) {
    if (foci.isEmpty()) {
      throw new IllegalArgumentException("At least one focus is required");
    }
    if (maxCarSpeedMetersPerSecond <= 0) {
      throw new IllegalArgumentException("maxCarSpeed must be positive");
    }
    this.focusLat = new double[foci.size()];
    this.focusLon = new double[foci.size()];
    this.boundSeconds = new long[foci.size()];
    for (int i = 0; i < foci.size(); i++) {
      focusLat[i] = foci.get(i).coordinate().y;
      focusLon[i] = foci.get(i).coordinate().x;
      boundSeconds[i] = foci.get(i).boundSeconds();
    }
    this.maxCarSpeedMetersPerSecond = maxCarSpeedMetersPerSecond;
  }

  /**
   * Whether a vertex at {@code (lat, lon)}, reached after {@code elapsedSeconds}, can still be on a
   * feasible detour of at least one of the legs.
   */
  boolean contains(long elapsedSeconds, double lat, double lon) {
    for (int i = 0; i < boundSeconds.length; i++) {
      if (elapsedSeconds + remainingLowerBound(lat, lon, i) <= boundSeconds[i]) {
        return true;
      }
    }
    return false;
  }

  /**
   * Seconds needed at least to drive from {@code (lat, lon)} to focus {@code i}. The fast distance
   * may over-estimate the great-circle distance by a fraction of a percent; scaling by
   * {@link SphericalDistanceLibrary#MAX_ERR_INV} keeps the estimate a lower bound.
   */
  private double remainingLowerBound(double lat, double lon, int i) {
    double meters =
      SphericalDistanceLibrary.fastDistance(lat, lon, focusLat[i], focusLon[i]) *
      SphericalDistanceLibrary.MAX_ERR_INV;
    return meters / maxCarSpeedMetersPerSecond;
  }
}
