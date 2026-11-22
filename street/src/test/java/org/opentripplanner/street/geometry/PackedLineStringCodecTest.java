package org.opentripplanner.street.geometry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

class PackedLineStringCodecTest {

  private static final double TOLERANCE = 0.0000015;

  @Test
  void roundTripPreservesAllCoordinatesIncludingEndpoints() {
    LineString original = makeLineString(-179.99, 1.12345, 10.123456, 59.654321, 179.99, 1.12345);
    byte[] packed = PackedLineStringCodec.pack(original);

    assertEquals(3, PackedLineStringCodec.countCoordinates(packed));
    assertTrue(original.equalsExact(PackedLineStringCodec.unpack(packed, false), TOLERANCE));
  }

  @Test
  void reverseUnpackMatchesReverseOfOriginal() {
    LineString original = makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0);
    byte[] packed = PackedLineStringCodec.pack(original);
    assertTrue(
      original.reverse().equalsExact(PackedLineStringCodec.unpack(packed, true), TOLERANCE)
    );
  }

  @Test
  void twoPointLineIsEncodedWithSentinels() {
    LineString original = makeLineString(10.0, 59.0, 10.5, 59.0);
    byte[] packed = PackedLineStringCodec.pack(original);
    assertThat(packed).isNotEmpty();
    assertTrue(original.equalsExact(PackedLineStringCodec.unpack(packed, false), TOLERANCE));
  }

  @Test
  void straightLineIsEmptyAndSharedInstance() {
    assertEquals(0, PackedLineStringCodec.countCoordinates(PackedLineStringCodec.STRAIGHT_LINE));
    assertTrue(PackedLineStringCodec.unpack(PackedLineStringCodec.STRAIGHT_LINE, false).isEmpty());
  }

  @Test
  void decodeIntoSkipsFirstCoordinateButKeepsAccumulating() {
    byte[] packed = PackedLineStringCodec.pack(makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0));
    double[] out = new double[4];
    int end = PackedLineStringCodec.decodeInto(packed, out, 0, 0, 0, true);
    assertEquals(4, end);
    assertEquals(10.1, out[0], TOLERANCE);
    assertEquals(59.0, out[3], TOLERANCE);
  }
}
