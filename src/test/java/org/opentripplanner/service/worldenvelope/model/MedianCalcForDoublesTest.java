package org.opentripplanner.service.worldenvelope.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MedianCalcForDoublesTest {

  @Test
  void medianOfOneNumbers() {
    var m = new MedianCalcForDoubles(1);
    m.add(7);
    assertEquals(7.0, m.median());
  }

  @Test
  void medianOfTwoNumbers() {
    var m = new MedianCalcForDoubles(2);
    m.add(7);
    m.add(3);
    assertEquals(5.0, m.median());
  }

  @Test
  void medianOf3Numbers() {
    var m = new MedianCalcForDoubles(3);
    m.add(2);
    m.add(7);
    m.add(1);
    assertEquals(2.0, m.median());
  }

  @Test
  void medianIncorrectSize() {
    var m = new MedianCalcForDoubles(2);
    m.add(3);
    assertThrows(IllegalStateException.class, m::median);
  }

  @Test
  void medianIncorrectInit() {
    assertThrows(IllegalStateException.class, () -> new MedianCalcForDoubles(0));
  }
}
