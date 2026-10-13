package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

class CompactLineStringTest {

  private static final double TOLERANCE = 0.0000015;

  @Test
  void ofEmptyReturnsSingleton() {
    assertSame(CompactLineString.STRAIGHT_LINE, CompactLineString.wrap(new byte[0]));
  }

  @Test
  void wrapNullReturnsNull() {
    assertNull(CompactLineString.wrap(null));
  }

  @Test
  void ofNullReturnsNull() {
    assertNull(CompactLineString.of(null));
  }

  @Test
  void roundTripPreservesAllCoordinatesIncludingEndpoints() {
    LineString original = makeLineString(-179.99, 1.12345, 10.123456, 59.654321, 179.99, 1.12345);
    CompactLineString g = CompactLineString.of(original);

    assertEquals(original.getNumPoints(), g.coordinateCount());
    LineString result = g.toLineString(false);
    assertEquals(original.getNumPoints(), result.getNumPoints());
    assertTrue(original.equalsExact(result, TOLERANCE));
  }

  @Test
  void reverseToLineStringMatchesReverseOfOriginal() {
    LineString original = makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0);
    CompactLineString g = CompactLineString.of(original);
    LineString reversed = g.toLineString(true);
    assertTrue(original.reverse().equalsExact(reversed, TOLERANCE));
  }

  @Test
  void twoPointLineRoundTrips() {
    LineString original = makeLineString(10.0, 59.0, 10.5, 59.0);
    CompactLineString g = CompactLineString.of(original);
    // compact() pads with (0,0) sentinels, so even a 2-point line is encoded (not the singleton).
    assertNotEquals(CompactLineString.STRAIGHT_LINE, g);
    LineString result = g.toLineString(false);
    assertEquals(2, result.getNumPoints());
    assertTrue(original.equalsExact(result, TOLERANCE));
  }

  @Test
  void equalsAndHashCodeAreContentBased() {
    LineString ls = makeLineString(10.0, 59.0, 10.1, 59.1, 10.2, 59.0);
    CompactLineString a = CompactLineString.of(ls);
    CompactLineString b = CompactLineString.of(ls);
    // Different instances, same content -> equal, same hashCode (enables interning).
    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());

    CompactLineString other = CompactLineString.of(makeLineString(20.0, 50.0, 20.5, 50.0));
    assertNotEquals(a, other);
  }

  @Test
  void straightLineSingletonHasZeroCoordinates() {
    assertEquals(0, CompactLineString.STRAIGHT_LINE.coordinateCount());
    assertTrue(CompactLineString.STRAIGHT_LINE.toLineString(false).isEmpty());
  }
}
