package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class LineStringShrinkerTest {

  @Test
  void horizontalLine() {
    var result = LineStringShrinker.shrink(new Coordinate(0, 0), new Coordinate(1, 0));
    assertEquals("LINESTRING (0.0001 0, 0.9999 0)", result.toString());
  }

  @Test
  void verticalLine() {
    var result = LineStringShrinker.shrink(new Coordinate(0, 0), new Coordinate(0, 1));
    assertEquals("LINESTRING (0 0.0001, 0 0.9999)", result.toString());
  }

  @Test
  void diagonalLine() {
    var result = LineStringShrinker.shrink(new Coordinate(0, 0), new Coordinate(1, 1));
    assertEquals("LINESTRING (0.0001 0.0001, 0.9999 0.9999)", result.toString());
  }

  @Test
  void shrunkLineIsShorterThanOriginal() {
    var from = new Coordinate(0, 0);
    var to = new Coordinate(3, 4);
    var result = LineStringShrinker.shrink(from, to);

    double originalLength = from.distance(to);
    assertTrue(result.getLength() < originalLength);
    assertEquals(0.9998 * originalLength, result.getLength(), 1e-9);
  }
}
