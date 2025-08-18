package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class ShapePointTest {

  @Test
  void equals() {
    var p1 = new ShapePoint(1, 2, 3, null);
    assertEquals(p1, p1);
  }

  @Test
  void notEquals() {
    var p1 = new ShapePoint(1, 2, 3, null);
    var p2 = new ShapePoint(1, 2, 3, null);
    var p3 = new ShapePoint(2, 2, 3, null);
    assertEquals(p1, p2);
    assertNotEquals(p1, p3);
    assertNotEquals(p2, p3);
  }
}
