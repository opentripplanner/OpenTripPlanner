package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class IntUtilsTest {

  @Test
  void intToString() {
    assertEquals("7", IntUtils.intToString(7, -1));
    assertEquals("", IntUtils.intToString(-1, -1));
  }

  @Test
  void intArray() {
    assertArrayEquals(new int[] { 5, 5, 5 }, IntUtils.intArray(3, 5));
  }

  @Test
  void concat() {
    assertArrayEquals(new int[] { 1, 2, 5, 6 }, IntUtils.concat(List.of(1, 2), List.of(5, 6)));
  }

  @Test
  void standardDeviation() {
    assertEquals(0.0, IntUtils.standardDeviation(List.of(7)));
    assertEquals(2.0, IntUtils.standardDeviation(List.of(9, 2, 4, 4, 5, 4, 7, 5)), 0.01);
  }
}
