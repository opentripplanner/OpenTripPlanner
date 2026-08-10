package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.LineString;

class EndpointContextLineStringTest {

  private static final double TOLERANCE = 0.0000015;

  private static final double X_0 = 1.111111111;
  private static final double Y_0 = 0.123456789;
  private static final double X_1 = 2.0;
  private static final double Y_1 = 0.0;

  @Test
  void twoPointLineUsesStraightLineSingleton() {
    LineString ls = makeLineString(X_0, Y_0, X_1, Y_1);
    byte[] coords = EndpointContextLineString.compact(X_0, Y_0, X_1, Y_1, ls, false);
    // ==, not equals: a 2-point line stores nothing, so it reuses the shared empty array.
    assertSame(EndpointContextLineString.STRAIGHT_LINE_PACKED, coords);
    LineString ls2 = EndpointContextLineString.uncompact(X_0, Y_0, X_1, Y_1, coords, false);
    assertTrue(ls.equalsExact(ls2, TOLERANCE));
  }

  @Test
  void roundTripWithReverse() {
    LineString ls = makeLineString(X_0, Y_0, -179.99, 1.12345, 179.99, 1.12345, X_1, Y_1);
    byte[] coords = EndpointContextLineString.compact(X_0, Y_0, X_1, Y_1, ls, false);
    assertNotSame(EndpointContextLineString.STRAIGHT_LINE_PACKED, coords);
    LineString ls2 = EndpointContextLineString.uncompact(X_0, Y_0, X_1, Y_1, coords, false);
    assertTrue(ls.equalsExact(ls2, TOLERANCE));

    // Reverse mode: encoding (B,A) with reverse=true yields the same bytes, and decoding with
    // reverse=true reproduces the reversed line string.
    LineString reversed = ls.reverse();
    byte[] coordsRev = EndpointContextLineString.compact(X_1, Y_1, X_0, Y_0, ls, true);
    assertEquals(coords.length, coordsRev.length);
    for (int i = 0; i < coords.length; i++) {
      assertEquals(coords[i], coordsRev[i]);
    }
    LineString ls3 = EndpointContextLineString.uncompact(X_1, Y_1, X_0, Y_0, coordsRev, true);
    assertTrue(reversed.equalsExact(ls3, TOLERANCE));
  }
}
