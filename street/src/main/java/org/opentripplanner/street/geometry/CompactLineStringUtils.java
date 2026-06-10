package org.opentripplanner.street.geometry;

import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;

/**
 * Endpoint-context glue around {@link CompactLineString} for callers (e.g. {@code StreetEdge}) whose
 * endpoints come from external context and are <i>omitted</i> from the packed form to save memory.
 * The Dlugosz/delta engine lives in {@link CompactLineString}; this class only handles the endpoint
 * splice/strip + the stick-to-endpoint check at compaction time.
 * <p>
 * For the self-contained contract (endpoints baked into the packed form) use {@link CompactLineString}
 * directly.
 *
 * @author laurent
 */
public final class CompactLineStringUtils {

  /**
   * Constant to check that line string end points are sticking to given points. 0.000001 is around
   * 1 meter at the equator. Do not use a too low value, ShapeFile builder has some rounding issues
   * and do not ensure perfect equality between endpoints and geometry.
   */
  private static final double EPS = 0.000001;

  /**
   * Singleton representation of a straight-line (where nothing has to be stored). Shares its
   * underlying byte array with {@link CompactLineString#STRAIGHT_LINE} so there is exactly one
   * empty-byte-array instance in the JVM.
   */
  static final byte[] STRAIGHT_LINE_PACKED = CompactLineString.STRAIGHT_LINE.packed();

  private CompactLineStringUtils() {}

  /**
   * Pack a line string into the endpoint-context form: the first and last coordinates are
   * <i>omitted</i> (they must equal the supplied {@code (xa,ya)} and {@code (xb,yb)} endpoints up
   * to {@link #EPS}) and only the intermediate points are stored, delta-coded against the start
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
  public static byte[] compactLineString(
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
        "CompactLineStringUtils geometry must stick to given end points. If you need to relax this, please read source code."
      );
    }
    int oix = CompactLineString.toFixedPoint(x0);
    int oiy = CompactLineString.toFixedPoint(y0);
    return CompactLineString.packIntermediateDeltas(lineString, oix, oiy);
  }

  /**
   * Decode a line string previously produced by
   * {@link #compactLineString(double, double, double, double, LineString, boolean)}, splicing the
   * supplied endpoints back around the decoded intermediate points.
   *
   * @param xa           X coordinate of end point A
   * @param ya           Y coordinate of end point A
   * @param xb           X coordinate of end point B
   * @param yb           Y coordinate of end point B
   * @param packedCoords The byte array to uncompact
   */
  public static LineString uncompactLineString(
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
}
