package org.opentripplanner.street.geometry;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

class PackedLineStringCodecTest {

  private static final double TOLERANCE = 0.0000015;

  @Test
  void roundTripPreservesAllCoordinatesIncludingEndpoints() {
    LineString original = makeLineString(-179.99, 1.12345, 10.123456, 59.654321, 179.99, 1.12345);
    byte[] packed = PackedLineStringCodec.pack(original);

    assertThat(PackedLineStringCodec.countCoordinates(packed)).isEqualTo(3);
    assertThat(
      original.equalsExact(PackedLineStringCodec.unpack(packed, false), TOLERANCE)
    ).isTrue();
  }

  @Test
  void reverseUnpackMatchesReverseOfOriginal() {
    LineString original = makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0);
    byte[] packed = PackedLineStringCodec.pack(original);
    assertThat(
      original.reverse().equalsExact(PackedLineStringCodec.unpack(packed, true), TOLERANCE)
    ).isTrue();
  }

  @Test
  void twoPointLineIsEncodedWithSentinels() {
    LineString original = makeLineString(10.0, 59.0, 10.5, 59.0);
    byte[] packed = PackedLineStringCodec.pack(original);
    assertThat(packed).isNotEmpty();
    assertThat(
      original.equalsExact(PackedLineStringCodec.unpack(packed, false), TOLERANCE)
    ).isTrue();
  }

  @Test
  void straightLineIsEmptyAndSharedInstance() {
    assertThat(
      PackedLineStringCodec.countCoordinates(PackedLineStringCodec.STRAIGHT_LINE)
    ).isEqualTo(0);
    assertThat(
      PackedLineStringCodec.unpack(PackedLineStringCodec.STRAIGHT_LINE, false).isEmpty()
    ).isTrue();
  }

  @Test
  void decodeIntoSkipsFirstCoordinateButKeepsAccumulating() {
    byte[] packed = PackedLineStringCodec.pack(makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0));
    double[] out = new double[4];
    int end = PackedLineStringCodec.decodeInto(packed, out, 0, 0, 0, true);
    assertThat(end).isEqualTo(4);
    assertThat(out[0]).isWithin(TOLERANCE).of(10.1);
    assertThat(out[3]).isWithin(TOLERANCE).of(59.0);
  }
}
