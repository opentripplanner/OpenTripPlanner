package org.opentripplanner.framework.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * Compact line string. To optimize storage, we use the following tricks:
 * <ul>
 * <li>Store only intermediate points (end-points are given by the external context, ie from/to
 * vertices)</li>
 * <li>For straight-line geometries (sometimes around half of the street geometries), re-use the
 * same static object (since there is nothing to store)</li>
 * <li>Store intermediate point in fixed floating points with fixed precision, using delta coding
 * from the previous point, and variable length coding (most of the delta coordinates will thus fits
 * in 1 or 2 bytes).</li>
 * </ul>
 * <p>
 * This trick alone saves around 20% of memory compared to the bulky JTS LineString, which is split
 * on many objects (Coordinates, cached Envelope, Geometry itself). Performance hit should be low as
 * we do not need the geometry during a path search.
 *
 * @author laurent
 */
public final class CompactLineStringUtils {

  /**
   * Multiplier for fixed-float representation. For lat/lon CRS, 1e6 leads to a precision of 0.11
   * meter at a minimum (at the equator).
   */
  private static final double FIXED_FLOAT_MULT = 1.0e6;

  /**
   * Constant to check that line string end points are sticking to given points. 0.000001 is around
   * 1 meter at the equator. Do not use a too low value, ShapeFile builder has some rounding issues
   * and do not ensure perfect equality between endpoints and geometry.
   */
  private static final double EPS = 0.000001;

  /**
   * Singleton representation of a straight-line (where nothing has to be stored), to be re-used.
   */
  public static final byte[] STRAIGHT_LINE_PACKED = new byte[0];

  /**
   * Public factory to create a compact line string. Optimize for straight-line only line string
   * (w/o intermediate end-points).
   *
   * @param xa         X coordinate of end point A
   * @param ya         Y coordinate of end point A
   * @param xb         X coordinate of end point B
   * @param yb         Y coordinate of end point B
   * @param lineString The geometry to compact. Please be aware that we ignore first and last
   *                   coordinate in the line string, they need to be exactly the same as A and B.
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
    if (lineString.getCoordinates().length == 2) {
      return STRAIGHT_LINE_PACKED;
    }
    double x0 = reverse ? xb : xa;
    double y0 = reverse ? yb : ya;
    double x1 = reverse ? xa : xb;
    double y1 = reverse ? ya : yb;
    Coordinate[] c = lineString.getCoordinates();
    /*
     * Check if the lineString is really sticking to the given end-points. TODO: If this is not
     * guaranteed, store all delta (from 0 to n-1) -- but how to mark it? A prefix?
     */
    if (
      Math.abs(x0 - c[0].x) > EPS ||
      Math.abs(y0 - c[0].y) > EPS ||
      Math.abs(x1 - c[c.length - 1].x) > EPS ||
      Math.abs(y1 - c[c.length - 1].y) > EPS
    ) throw new IllegalArgumentException(
      "CompactLineStringUtils geometry must stick to given end points. If you need to relax this, please read source code."
    );
    int oix = IntUtils.round(x0 * FIXED_FLOAT_MULT);
    int oiy = IntUtils.round(y0 * FIXED_FLOAT_MULT);
    int[] coords = new int[(c.length - 2) * 2];
    for (int i = 1; i < c.length - 1; i++) {
      /*
       * Note: We should do the rounding *before* the delta to prevent rounding errors from
       * accumulating on long line strings.
       */
      int ix = IntUtils.round(c[i].x * FIXED_FLOAT_MULT);
      int iy = IntUtils.round(c[i].y * FIXED_FLOAT_MULT);
      int dix = ix - oix;
      int diy = iy - oiy;
      coords[(i - 1) * 2] = dix;
      coords[(i - 1) * 2 + 1] = diy;
      oix = ix;
      oiy = iy;
    }
    return DlugoszVarLenIntPacker.pack(coords);
  }

  /**
   * Wrapper for the above method in the case where there are no start/end coordinates provided.
   * 0-coordinates are added in order for the delta encoding to work correctly.
   */
  public static byte[] compactLineString(LineString lineString, boolean reverse) {
    if (lineString == null) {
      return null;
    }
    lineString = GeometryUtils.addStartEndCoordinatesToLineString(
      new Coordinate(0.0, 0.0),
      lineString,
      new Coordinate(0.0, 0.0)
    );
    return compactLineString(0.0, 0.0, 0.0, 0.0, lineString, reverse);
  }

  /**
   * Same as the other version, but in a var-len int packed form (Dlugosz coding).
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
    int[] coords = DlugoszVarLenIntPacker.unpack(packedCoords);
    int size = coords == null ? 2 : (coords.length / 2) + 2;
    Coordinate[] c = new Coordinate[size];
    double x0 = reverse ? xb : xa;
    double y0 = reverse ? yb : ya;
    double x1 = reverse ? xa : xb;
    double y1 = reverse ? ya : yb;
    c[0] = new Coordinate(x0, y0);
    if (coords != null) {
      int oix = IntUtils.round(x0 * FIXED_FLOAT_MULT);
      int oiy = IntUtils.round(y0 * FIXED_FLOAT_MULT);
      for (int i = 1; i < c.length - 1; i++) {
        int ix = oix + coords[(i - 1) * 2];
        int iy = oiy + coords[(i - 1) * 2 + 1];
        c[i] = new Coordinate(ix / FIXED_FLOAT_MULT, iy / FIXED_FLOAT_MULT);
        oix = ix;
        oiy = iy;
      }
    }
    c[c.length - 1] = new Coordinate(x1, y1);
    LineString out = GeometryUtils.makeLineString(c);
    if (reverse) {
      out = out.reverse();
    }
    return out;
  }

  /**
   * Wrapper for the above method in the case where there are no start/end coordinates provided.
   * 0-coordinates are added and then removed in order for the delta encoding to work correctly.
   * Same as the other version, but in a var-len int packed form (Dlugosz coding).
   */
  public static LineString uncompactLineString(byte[] packedCoords, boolean reverse) {
    LineString lineString = uncompactLineString(0.0, 0.0, 0.0, 0.0, packedCoords, reverse);
    return GeometryUtils.removeStartEndCoordinatesFromLineString(lineString);
  }
}
