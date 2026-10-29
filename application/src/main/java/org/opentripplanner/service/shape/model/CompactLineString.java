package org.opentripplanner.service.shape.model;

import java.io.Serializable;
import java.util.Arrays;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.PackedLineStringCodec;

/**
 * A single delta-packed line string, <b>self-contained</b>: the line string's endpoints are
 * encoded into the packed form, so decode is fully described by the bytes alone. This is the
 * geometry of one hop between two stops.
 * <p>
 * Instances are immutable value objects with content based {@link #equals(Object)} and
 * {@link #hashCode()}, which enables deduplication: identical hops share one instance.
 */
final class CompactLineString implements Serializable {

  /**
   * Shared instance for a 2-point straight line (nothing to store).
   */
  static final CompactLineString STRAIGHT_LINE = new CompactLineString(
    PackedLineStringCodec.STRAIGHT_LINE
  );

  private final byte[] packed;

  private CompactLineString(byte[] packed) {
    this.packed = packed;
  }

  /**
   * Wrap an already-packed byte array. Returns the shared {@link #STRAIGHT_LINE} singleton for an
   * empty array. {@code null} input maps to {@code null}.
   */
  @Nullable
  static CompactLineString wrap(@Nullable byte[] packed) {
    if (packed == null) {
      return null;
    }
    return packed.length == 0 ? STRAIGHT_LINE : new CompactLineString(packed);
  }

  /**
   * Compact a line string. {@code null} input maps to {@code null}.
   */
  @Nullable
  static CompactLineString of(@Nullable LineString lineString) {
    if (lineString == null) {
      return null;
    }
    return wrap(PackedLineStringCodec.pack(lineString));
  }

  /**
   * Decode this line string.
   *
   * @param reverse if {@code true}, the resulting line string is reversed.
   */
  LineString toLineString(boolean reverse) {
    return PackedLineStringCodec.unpack(packed, reverse);
  }

  /**
   * Number of coordinates encoded, without decoding them.
   */
  int coordinateCount() {
    return PackedLineStringCodec.countCoordinates(packed);
  }

  /**
   * Package-private accessor for {@link CompactLineStringSequence}.
   */
  byte[] packed() {
    return packed;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CompactLineString g && Arrays.equals(packed, g.packed);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(packed);
  }
}
