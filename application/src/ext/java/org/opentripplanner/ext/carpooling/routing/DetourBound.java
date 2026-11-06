package org.opentripplanner.ext.carpooling.routing;

import java.time.Duration;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * The bound on a detour through a point {@code v} on a driver leg from {@code a} to {@code b}: the
 * leg can only pass through {@code v} if {@code t(a→v) + t(v→b)} is at most the leg's limit, see
 * {@link CarpoolCorridor#legLimits()}. A side whose drive is not known counts as its fastest
 * possible drive, the beeline at the fastest car speed in the graph, so the bound never rules out a
 * feasible detour.
 */
final class DetourBound {

  /** Margin on the bounding box, against the approximations in degree conversion. */
  private static final double ENVELOPE_MARGIN = 1.02;

  private final Vertex start;
  private final Vertex end;
  private final Duration limit;
  private final long limitSeconds;
  private final double maxCarSpeed;

  /** @param maxCarSpeed the fastest speed any street in the graph can be driven at, in m/s */
  DetourBound(Vertex start, Vertex end, Duration limit, double maxCarSpeed) {
    if (maxCarSpeed <= 0) {
      throw new IllegalArgumentException("maxCarSpeed must be positive");
    }
    this.start = start;
    this.end = end;
    this.limit = limit;
    this.limitSeconds = limit.toSeconds();
    this.maxCarSpeed = maxCarSpeed;
  }

  /**
   * Whether a detour through the point can fit, with both sides at their fastest possible drive.
   */
  boolean mayPassThrough(Vertex point) {
    return mayPassWithin(0, point);
  }

  /** Whether a detour can pass anywhere within {@code meters} of the point. */
  boolean mayPassWithin(double meters, Vertex point) {
    double slack = (2 * meters) / maxCarSpeed;
    double drive = fastestPossibleDriveFromStart(point) + fastestPossibleDriveToEnd(point);
    return drive <= limitSeconds + slack;
  }

  /** Whether a detour whose two sides take these many seconds fits. */
  boolean fits(long secondsFromStart, long secondsToEnd) {
    return secondsFromStart + secondsToEnd <= limitSeconds;
  }

  /**
   * A bounding box around every point a detour can pass through.
   */
  Envelope envelope() {
    double halfMeters = ((limitSeconds * maxCarSpeed) / 2.0) * ENVELOPE_MARGIN;
    double midLat = (start.getLat() + end.getLat()) / 2.0;
    double midLon = (start.getLon() + end.getLon()) / 2.0;
    return SphericalDistanceLibrary.bounds(midLat, midLon, halfMeters, halfMeters);
  }

  /** Whether a detour that reaches the point {@code secondsFromStart} into the leg can fit. */
  boolean mayPassThroughAfter(long secondsFromStart, Vertex point) {
    return secondsFromStart + fastestPossibleDriveToEnd(point) <= limitSeconds;
  }

  /** Whether a detour that leaves the point {@code secondsToEnd} before the leg's end can fit. */
  boolean mayPassThroughBefore(long secondsToEnd, Vertex point) {
    return secondsToEnd + fastestPossibleDriveFromStart(point) <= limitSeconds;
  }

  /**
   * Skips every edge of a search of the leg into a vertex no detour can pass through, which keeps
   * the search within an ellipse with the leg's ends as foci. A forward search is rooted at the
   * leg's start, a reverse search at its end; the vertex an edge leads to is its from-vertex in a
   * reverse search.
   */
  SkipEdgeStrategy<State, Edge> skipEdgeStrategy(boolean reverse) {
    if (reverse) {
      return (current, edge) ->
        !mayPassThroughBefore(current.getElapsedTimeSeconds(), edge.getFromVertex());
    }
    return (current, edge) ->
      !mayPassThroughAfter(current.getElapsedTimeSeconds(), edge.getToVertex());
  }

  /**
   * The most the leg may take from the point on: the limit minus the fastest possible drive to the
   * point.
   */
  Duration limitFrom(Vertex point) {
    return limit.minusSeconds((long) fastestPossibleDriveFromStart(point));
  }

  /**
   * The most the leg may take up to the point: the limit minus the fastest possible drive from the
   * point on.
   */
  Duration limitTo(Vertex point) {
    return limit.minusSeconds((long) fastestPossibleDriveToEnd(point));
  }

  private double fastestPossibleDriveFromStart(Vertex point) {
    return fastestPossibleDrive(start, point);
  }

  private double fastestPossibleDriveToEnd(Vertex point) {
    return fastestPossibleDrive(point, end);
  }

  private double fastestPossibleDrive(Vertex from, Vertex to) {
    double meters = SphericalDistanceLibrary.fastDistance(
      from.getLat(),
      from.getLon(),
      to.getLat(),
      to.getLon()
    );
    return meters / maxCarSpeed;
  }
}
