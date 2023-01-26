package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  void allEquals() {
    assertTrue(IntUtils.arrayEquals(new int[] { 5 }, 5));
    assertTrue(IntUtils.arrayEquals(new int[] { 5, 5, 5 }, 5));
    assertTrue(IntUtils.arrayEquals(new int[] {}, 5));
    assertFalse(IntUtils.arrayEquals(new int[] { 2 }, 1));
    assertFalse(IntUtils.arrayEquals(new int[] { 5, 2 }, 5));
  }

  @Test
  void arrayPlus() {
    assertArrayEquals(new int[] { 10 }, IntUtils.arrayPlus(new int[] { 5 }, 5));
    assertArrayEquals(new int[] { 0, -4, -12 }, IntUtils.arrayPlus(new int[] { 5, 1, -7 }, -5));
    assertArrayEquals(new int[0], IntUtils.arrayPlus(new int[0], 5));
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
