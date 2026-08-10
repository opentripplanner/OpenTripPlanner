package org.opentripplanner.street.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

/**
 * Shrinks a line segment slightly at both ends to make point-in-polygon containment tests robust
 * against floating-point boundary ambiguity.
 */
public class LineStringShrinker {

  /**
   * Factor by which a candidate segment is shrunk at each end before the containment test. Floating
   * point math cannot represent boundaries precisely, so the segment between two boundary points is
   * pulled slightly inward to make the test robust.
   */
  private static final double AREA_INTERSECTION_SHRINKING = 0.0001;

  /**
   * Returns a new {@link LineString} between {@code from} and {@code to} with each endpoint pulled
   * inward by {@link #AREA_INTERSECTION_SHRINKING} times the segment length. The resulting segment
   * lies strictly inside the original, avoiding false negatives when the endpoints sit exactly on a
   * polygon boundary.
   */
  public static LineString shrink(Coordinate from, Coordinate to) {
    var dx = AREA_INTERSECTION_SHRINKING * (to.x - from.x);
    var dy = AREA_INTERSECTION_SHRINKING * (to.y - from.y);
    return GeometryUtils.makeLineString(from.x + dx, from.y + dy, to.x - dx, to.y - dy);
  }
}
