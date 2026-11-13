package org.opentripplanner.street.geometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.utils.lang.IntUtils;

/**
 * The single home of the delta-packed line string encode/decode engine. Coordinates are stored as
 * fixed-point deltas packed with {@link DlugoszVarLenIntPacker}.
 * <p>
 * Two contracts are built on top of the engine:
 * <ul>
 *   <li><b>Self-contained</b> ({@link #pack(LineString)} / {@link #unpack(byte[], boolean)}): the
 *       endpoints are encoded into the bytes by padding with {@code (0,0)} sentinels, so decode is
 *       fully described by the bytes alone.</li>
 *   <li><b>Endpoint-context</b> ({@link EndpointContextLineString}): the endpoints are omitted from
 *       the bytes and re-supplied at decode time from external context.</li>
 * </ul>
 */
public final class PackedLineStringCodec {

  /**
   * Multiplier for fixed-float representation. For lat/lon CRS, 1e6 leads to a precision of 0.11
   * meter at a minimum (at the equator).
   */
  private static final double FIXED_FLOAT_MULT = 1.0e6;

  /**
   * Shared packed form of a 2-point straight line (nothing to store). Reused everywhere a straight
   * line is encoded, so there is exactly one empty-byte-array instance in the JVM. It must never
   * be modified.
   */
  public static final byte[] STRAIGHT_LINE = new byte[0];

  private PackedLineStringCodec() {}

  /**
   * Encode a floating-point coordinate as a fixed-point integer at the precision implied by
   * {@code FIXED_FLOAT_MULT}. Rounds to the nearest integer.
   */
  public static int toFixedPoint(double v) {
    return IntUtils.round(v * FIXED_FLOAT_MULT);
  }

  /**
   * Decode a fixed-point integer back to the corresponding floating-point coordinate.
   */
  public static double toFloatingPoint(int v) {
    return v / FIXED_FLOAT_MULT;
  }

  /**
   * Pack a line string with no external endpoint context: endpoints are encoded into the packed
   * form by padding the input with two {@code (0,0)} sentinels and delta-encoding every original
   * coordinate against that origin. The result is never {@code null}.
   */
  public static byte[] pack(LineString lineString) {
    LineString padded = GeometryUtils.addStartEndCoordinatesToLineString(
      new Coordinate(0.0, 0.0),
      lineString,
      new Coordinate(0.0, 0.0)
    );
    byte[] packed = packIntermediateDeltas(padded, 0, 0);
    return packed.length == 0 ? STRAIGHT_LINE : packed;
  }

  /**
   * Decode a line string packed with {@link #pack(LineString)}. The deltas are accumulated from
   * fixed-point origin {@code (0,0)} so every original coordinate (including endpoints)
   * round-trips.
   *
   * @param reverse if {@code true}, the resulting line string is reversed.
   */
  public static LineString unpack(byte[] packed, boolean reverse) {
    int n = countCoordinates(packed);
    if (n == 0) {
      return GeometryUtils.emptyLineString();
    }
    double[] c = new double[n * 2];
    decodeInto(packed, c, 0, 0, 0, false);
    LineString out = GeometryUtils.makeLineString(c);
    return reverse ? out.reverse() : out;
  }

  /** Number of coordinates encoded in {@code packed}, without decoding them. */
  public static int countCoordinates(byte[] packed) {
    return DlugoszVarLenIntPacker.countValues(packed) / 2;
  }

  /**
   * Decode delta-encoded coordinate pairs from {@code packed} into {@code out} starting at
   * {@code offset}, accumulating from fixed-point origin {@code (oix, oiy)}. Returns the new write
   * offset (one past the last double written).
   * <p>
   * Streams through the packed bytes without allocating an intermediate {@code int[]} or boxing.
   *
   * @param packed         the packed line string, or {@code null} / empty for a no-op
   * @param out            target array to write decoded coordinates into (flat: [x0, y0, x1, y1, …])
   * @param offset         starting index in {@code out} to write to
   * @param oix            initial x in fixed-point (start of delta chain)
   * @param oiy            initial y in fixed-point (start of delta chain)
   * @param skipFirstCoord skip writing the first decoded coordinate. Deltas still accumulate so the
   *                       remaining coordinates land at their correct absolute positions. Used to
   *                       drop the shared seam coord when concatenating consecutive line strings.
   * @return the offset of the first unwritten slot in {@code out}
   */
  public static int decodeInto(
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
}
