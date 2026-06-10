package org.opentripplanner.street.geometry;

import java.io.Serializable;
import java.util.Arrays;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * A single delta-packed line string.
 * <p>
 * The same packed-bytes format is shared by two encoding contracts:
 * <ul>
 *   <li><b>self-contained</b> &mdash; the line string's endpoints are encoded into the packed form
 *       (by padding with {@code (0,0)} sentinels). Decode is fully described by the bytes alone.
 *       Exposed here as {@link #of(LineString)} / {@link #toLineString(boolean)}. Used by
 *       {@link CompactLineStringSequence}.</li>
 *   <li><b>endpoint-context</b> &mdash; the line string's endpoints are <i>omitted</i> from the
 *       packed form and re-supplied at decode time from external context (e.g. a street edge's
 *       from/to vertex). See {@link CompactLineStringUtils}
 *   </li>
 * </ul>
 */
public final class CompactLineString implements Serializable {

  /**
   * Multiplier for fixed-float representation. For lat/lon CRS, 1e6 leads to a precision of 0.11
   * meter at a minimum (at the equator).
   */
  private static final double FIXED_FLOAT_MULT = 1.0e6;

  /**
   * Encode a floating-point coordinate as a fixed-point integer at the precision implied by the
   * codec's {@code FIXED_FLOAT_MULT}. Rounds to the nearest integer.
   */
  static int toFixedPoint(double v) {
    return IntUtils.round(v * FIXED_FLOAT_MULT);
  }

  /**
   * Decode a fixed-point integer back to the corresponding floating-point coordinate.
   */
  static double toFloatingPoint(int v) {
    return v / FIXED_FLOAT_MULT;
  }

  /**
   * Shared instance for a 2-point straight line (nothing to store). Reused everywhere a straight
   * line is encoded, both in self-contained and endpoint-context contexts.
   */
  public static final CompactLineString STRAIGHT_LINE = new CompactLineString(new byte[0]);

  private final byte[] packed;

  private CompactLineString(byte[] packed) {
    this.packed = packed;
  }

  /**
   * Wrap an already-packed byte array. Returns the shared {@link #STRAIGHT_LINE} singleton for an
   * empty array. {@code null} input maps to {@code null}.
   */
  static CompactLineString wrap(byte[] packed) {
    if (packed == null) {
      return null;
    }
    return packed.length == 0 ? STRAIGHT_LINE : new CompactLineString(packed);
  }

  /**
   * Compact a line string with no external endpoint context: endpoints are encoded into the packed
   * form by padding the input with two {@code (0,0)} sentinels and delta-encoding every original
   * coordinate against that origin.
   */
  public static CompactLineString of(LineString lineString) {
    if (lineString == null) {
      return null;
    }
    LineString padded = GeometryUtils.addStartEndCoordinatesToLineString(
      new Coordinate(0.0, 0.0),
      lineString,
      new Coordinate(0.0, 0.0)
    );
    return wrap(packIntermediateDeltas(padded, 0, 0));
  }

  /**
   * Decode this self-contained packed line string. The deltas are accumulated from fixed-point
   * origin {@code (0,0)} so every original coordinate (including endpoints) round-trips.
   *
   * @param reverse if {@code true}, the resulting line string is reversed.
   */
  public LineString toLineString(boolean reverse) {
    int n = coordinateCount();
    if (n == 0) {
      return GeometryUtils.emptyLineString();
    }
    double[] c = new double[n * 2];
    decodeInto(packed, c, 0, 0, 0, false);
    LineString out = GeometryUtils.makeLineString(c);
    return reverse ? out.reverse() : out;
  }

  /** Number of coordinates encoded, without decoding them. */
  public int coordinateCount() {
    return DlugoszVarLenIntPacker.countValues(packed) / 2;
  }

  /** Package-private accessor for the endpoint-context glue and {@link CompactLineStringSequence}. */
  byte[] packed() {
    return packed;
  }

  // ---- the single copy of the decode/encode engine ---------------------------------------

  /**
   * Decode delta-encoded coordinate pairs from {@code packed} into {@code out} starting at
   * {@code offset}, accumulating from fixed-point origin {@code (oix, oiy)}. Returns the new write
   * offset (one past the last double written).
   * <p>
   * Streams through the packed bytes without allocating an intermediate {@code int[]} or boxing.
   * This is the single home of the decode logic; both the self-contained decode here and the
   * endpoint-context decode in {@link CompactLineStringUtils} call it.
   *
   * @param packed         the compact line string, or {@code null} / empty for a no-op
   * @param out            target array to write decoded coordinates into (flat: [x0, y0, x1, y1, …])
   * @param offset         starting index in {@code out} to write to
   * @param oix            initial x in fixed-point (start of delta chain)
   * @param oiy            initial y in fixed-point (start of delta chain)
   * @param skipFirstCoord skip writing the first decoded coordinate. Deltas still accumulate so the
   *                       remaining coordinates land at their correct absolute positions. Used to
   *                       drop the shared seam coord at hop seams in transit-pattern leg-geometry
   *                       concatenation.
   * @return the offset of the first unwritten slot in {@code out}
   */
  static int decodeInto(
    byte[] packed,
    double[] out,
    int offset,
    int oix,
    int oiy,
    boolean skipFirstCoord
  ) {
    if (packed == null || packed.length == 0) {
      return offset;
    }
    int writeIdx = offset;
    boolean skip = skipFirstCoord;
    var decoder = new DlugoszVarLenIntPacker.Decoder(packed);
    while (decoder.hasNext()) {
      oix += decoder.next();
      oiy += decoder.next();
      if (skip) {
        skip = false;
        continue;
      }
      out[writeIdx++] = toFloatingPoint(oix);
      out[writeIdx++] = toFloatingPoint(oiy);
    }
    return writeIdx;
  }

  /**
   * Encode the <i>intermediate</i> points of {@code lineString} (indices 1..n-2) as fixed-point
   * deltas accumulated from {@code (oix, oiy)} and produce the Dlugosz-packed bytes. The first and
   * last coordinates of the input are <b>not</b> stored.
   * <p>
   * Single home of the encode logic; both the self-contained {@link #of(LineString)} (which
   * passes a padded line string starting and ending at {@code (0,0)}) and the endpoint-context
   * compact in {@link CompactLineStringUtils} (which passes the vertex coordinates as origin)
   * delegate here.
   */
  static byte[] packIntermediateDeltas(LineString lineString, int oix, int oiy) {
    CoordinateSequence seq = lineString.getCoordinateSequence();
    int n = seq.size();
    int[] coords = new int[(n - 2) * 2];
    for (int i = 1; i < n - 1; i++) {
      // Round to fixed point before taking the delta to prevent rounding errors from accumulating.
      int ix = toFixedPoint(seq.getX(i));
      int iy = toFixedPoint(seq.getY(i));
      coords[(i - 1) * 2] = ix - oix;
      coords[(i - 1) * 2 + 1] = iy - oiy;
      oix = ix;
      oiy = iy;
    }
    return DlugoszVarLenIntPacker.pack(coords);
  }

  // ---- value semantics: enables sharing/interning of identical geometries -----------------

  @Override
  public boolean equals(Object o) {
    return o instanceof CompactLineString g && Arrays.equals(packed, g.packed);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(packed);
  }
}
