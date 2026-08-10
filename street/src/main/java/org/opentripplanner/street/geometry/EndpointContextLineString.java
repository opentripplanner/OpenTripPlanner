package org.opentripplanner.street.geometry;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

/**
 * Endpoint-context packed line strings: the first and last coordinates are <i>omitted</i> from the
 * packed bytes and re-supplied at decode time from external context (e.g. a street edge's from/to
 * vertices). Only the intermediate points are delta-packed, which keeps both the per-edge heap
 * footprint and the serialized graph minimal.
 * <p>
 * This is the stateless companion to {@link CompactLineString}. It operates on a raw {@code byte[]}
 * supplied by the caller plus the externally-held endpoints &mdash; {@code StreetEdge} stores the
 * bytes directly and supplies its from/to vertex coordinates per call, so there is no wrapper
 * object per edge.
 */
public final class EndpointContextLineString {

  /**
   * Constant to check that line string end points are sticking to given points. 0.000001 is around
   * 1 meter at the equator. Do not use a too low value, ShapeFile builder has some rounding issues
   * and do not ensure perfect equality between endpoints and geometry.
   */
  private static final double EPS = 0.000001;

  /**
   * Empty packed byte array for a straight line. Shares the underlying array with
   * {@link CompactLineString#STRAIGHT_LINE} so there is exactly one empty-byte-array instance in
   * the JVM.
   */
  static final byte[] STRAIGHT_LINE_PACKED = CompactLineString.STRAIGHT_LINE.packed();

  private EndpointContextLineString() {}

  /**
   * Pack a line string into the endpoint-context form: the first and last coordinates are
   * <i>omitted</i> (they must equal the supplied {@code (xa,ya)} and {@code (xb,yb)} endpoints up
   * to
   * {@link #EPS}) and only the intermediate points are stored, delta-coded against the start
   * endpoint.
   *
   * @param xa         X coordinate of end point A
   * @param ya         Y coordinate of end point A
   * @param xb         X coordinate of end point B
   * @param yb         Y coordinate of end point B
   * @param lineString The geometry to compact. The first and last coordinate must equal A and B
   *                   (or, when {@code reverse} is true, B and A).
   * @param reverse    True if A and B are inverted (B is start, A is end).
   */
  public static byte[] compact(
    double xa,
    double ya,
    double xb,
    double yb,
    LineString lineString,
    boolean reverse
  ) {
    if (lineString == null) {
      return null;
    }
    if (lineString.getNumPoints() == 2) {
      return STRAIGHT_LINE_PACKED;
    }
    double x0 = reverse ? xb : xa;
    double y0 = reverse ? yb : ya;
    double x1 = reverse ? xa : xb;
    double y1 = reverse ? ya : yb;
    CoordinateSequence seq = lineString.getCoordinateSequence();
    int n = seq.size();
    /*
     * Check if the lineString is really sticking to the given end-points. TODO: If this is not
     * guaranteed, store all delta (from 0 to n-1) -- but how to mark it? A prefix?
     */
    if (
      Math.abs(x0 - seq.getX(0)) > EPS ||
      Math.abs(y0 - seq.getY(0)) > EPS ||
      Math.abs(x1 - seq.getX(n - 1)) > EPS ||
      Math.abs(y1 - seq.getY(n - 1)) > EPS
    ) {
      throw new IllegalArgumentException(
        "EndpointContextLineString geometry must stick to given end points. If you need to relax this, please read source code."
      );
    }
    int oix = CompactLineString.toFixedPoint(x0);
    int oiy = CompactLineString.toFixedPoint(y0);
    return CompactLineString.packIntermediateDeltas(lineString, oix, oiy);
  }

  /**
   * Decode a line string previously produced by
   * {@link #compact(double, double, double, double, LineString, boolean)}, splicing the supplied
   * endpoints back around the decoded intermediate points.
   *
   * @param xa           X coordinate of end point A
   * @param ya           Y coordinate of end point A
   * @param xb           X coordinate of end point B
   * @param yb           Y coordinate of end point B
   * @param packedCoords The byte array to uncompact
   */
  public static LineString uncompact(
    double xa,
    double ya,
    double xb,
    double yb,
    byte[] packedCoords,
    boolean reverse
  ) {
    double x0 = reverse ? xb : xa;
    double y0 = reverse ? yb : ya;
    double x1 = reverse ? xa : xb;
    double y1 = reverse ? ya : yb;
    int intermediateCount = packedCoords == null
      ? 0
      : DlugoszVarLenIntPacker.countValues(packedCoords) / 2;
    double[] c = new double[(intermediateCount + 2) * 2];
    c[0] = x0;
    c[1] = y0;
    if (intermediateCount > 0) {
      int oix = CompactLineString.toFixedPoint(x0);
      int oiy = CompactLineString.toFixedPoint(y0);
      CompactLineString.decodeInto(packedCoords, c, 2, oix, oiy, false);
    }
    c[c.length - 2] = x1;
    c[c.length - 1] = y1;
    LineString out = GeometryUtils.makeLineString(c);
    return reverse ? out.reverse() : out;
  }

  /**
   * Squared distance from a point to an endpoint-context compacted line string, computed directly
   * on the packed form without materializing a {@link LineString} or any
   * {@link org.locationtech.jts.geom.Coordinate}.
   * <p>
   * This uses the "fast somewhat inaccurate" local equirectangular projection (only the x axis is
   * scaled by {@code xscale}; distances come out in latitude degrees) and computes the same
   * per-segment point-to-segment distance JTS {@code DistanceOp} produces &mdash; mathematically
   * equivalent up to floating-point rounding &mdash; but it streams the segments straight from the
   * packed deltas via {@link DlugoszVarLenIntPacker.Decoder} instead of decoding into an array
   * first.
   * <p>
   * The <b>square</b> of the distance is returned: the linker only ever orders edges and applies
   * thresholds, both of which are monotonic in the distance, so the per-candidate {@code sqrt} is
   * unnecessary (callers square their thresholds instead).
   * <p>
   * The result is order-invariant, so {@code reverse} only affects which endpoint seeds the delta
   * chain (it must match the value the bytes were encoded with); the minimum distance over the
   * segment set is identical regardless.
   *
   * @param xa      X (longitude) of end point A
   * @param ya      Y (latitude) of end point A
   * @param xb      X (longitude) of end point B
   * @param yb      Y (latitude) of end point B
   * @param packedCoords the endpoint-context packed intermediate points (may be empty/null for a
   *                     straight line)
   * @param reverse True if A and B are inverted (B is start, A is end) — must match compaction.
   * @param px      X (longitude) of the query point
   * @param py      Y (latitude) of the query point
   * @param xscale  cos(latitude) of the projection centre, as used by the linker
   * @return the squared projected distance in latitude degrees squared
   */
  public static double squaredEquirectangularDistanceToPoint(
    double xa,
    double ya,
    double xb,
    double yb,
    byte[] packedCoords,
    boolean reverse,
    double px,
    double py,
    double xscale
  ) {
    double x0 = reverse ? xb : xa;
    double y0 = reverse ? yb : ya;
    double x1 = reverse ? xa : xb;
    double y1 = reverse ? ya : yb;

    double pxs = px * xscale;
    double prevX = x0 * xscale;
    double prevY = y0;
    double minDistSq = Double.POSITIVE_INFINITY;

    if (packedCoords != null && packedCoords.length > 0) {
      int oix = CompactLineString.toFixedPoint(x0);
      int oiy = CompactLineString.toFixedPoint(y0);
      var decoder = new DlugoszVarLenIntPacker.Decoder(packedCoords);
      while (decoder.hasNext()) {
        oix += decoder.next();
        oiy += decoder.next();
        double curX = CompactLineString.toFloatingPoint(oix) * xscale;
        double curY = CompactLineString.toFloatingPoint(oiy);
        double distSq = segmentDistanceSq(pxs, py, prevX, prevY, curX, curY);
        if (distSq < minDistSq) {
          minDistSq = distSq;
        }
        prevX = curX;
        prevY = curY;
      }
    }

    // Final implicit segment to the other endpoint (the only segment for a straight line).
    double endX = x1 * xscale;
    double distSq = segmentDistanceSq(pxs, py, prevX, prevY, endX, y1);
    if (distSq < minDistSq) {
      minDistSq = distSq;
    }
    return minDistSq;
  }

  /**
   * Squared euclidean distance from point {@code (px,py)} to the segment {@code (ax,ay)-(bx,by)} in
   * the projected plane. Computes the same minimum distance JTS produces per segment, via the
   * project-onto-segment-and-clamp formula (equivalent to JTS up to floating-point rounding).
   */
  private static double segmentDistanceSq(
    double px,
    double py,
    double ax,
    double ay,
    double bx,
    double by
  ) {
    double dx = bx - ax;
    double dy = by - ay;
    double lengthSq = dx * dx + dy * dy;
    double t = lengthSq == 0.0 ? 0.0 : ((px - ax) * dx + (py - ay) * dy) / lengthSq;
    if (t < 0.0) {
      t = 0.0;
    } else if (t > 1.0) {
      t = 1.0;
    }
    double closestX = ax + t * dx;
    double closestY = ay + t * dy;
    double ex = px - closestX;
    double ey = py - closestY;
    return ex * ex + ey * ey;
  }
}
