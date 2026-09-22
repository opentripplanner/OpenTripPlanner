package org.opentripplanner.ext.carpooling.routing;

import static org.opentripplanner.ext.carpooling.util.BeelineEstimator.minDriveSeconds;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

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

  /**
   * Whether a vertex at {@code (lat, lon)}, reached after {@code elapsedSeconds}, can still be on a
   * feasible detour.
   */
  boolean contains(long elapsedSeconds, double lat, double lon) {
    double toFocus = minDriveSeconds(lat, lon, focusLat, focusLon, maxCarSpeedMetersPerSecond);
    return elapsedSeconds + toFocus <= boundSeconds;
  }

  /**
   * Skips every edge into a vertex outside the ellipse; the vertex an edge leads to is its
   * from-vertex in a reverse search.
   */
  SkipEdgeStrategy<State, Edge> skipEdgeStrategy(boolean reverse) {
    return (current, edge) -> {
      Vertex target = reverse ? edge.getFromVertex() : edge.getToVertex();
      return !contains(current.getElapsedTimeSeconds(), target.getLat(), target.getLon());
    };
  }
}
