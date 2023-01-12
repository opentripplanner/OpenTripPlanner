package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  @Test
  void hexToReadableInt() {
    assertEquals(0, IntUtils.hexToReadableInt("0"));
    assertEquals(1, IntUtils.hexToReadableInt("1"));
    assertEquals(10, IntUtils.hexToReadableInt("a"));
    assertEquals(15, IntUtils.hexToReadableInt("F"));
    assertEquals(100, IntUtils.hexToReadableInt("10"));
    assertEquals(1000000, IntUtils.hexToReadableInt("1000"));
    assertEquals(15071001, IntUtils.hexToReadableInt("F7a1"));

    // 5 digits is too long
    assertThrows(IllegalArgumentException.class, () -> IntUtils.hexToReadableInt("10000"));

    // None HEX character error
    assertThrows(IllegalArgumentException.class, () -> IntUtils.hexToReadableInt(""));
    assertThrows(IllegalArgumentException.class, () -> IntUtils.hexToReadableInt("G"));
  }
}
