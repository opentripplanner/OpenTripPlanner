package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.framework.lang.IntUtils.concat;
import static org.opentripplanner.framework.lang.IntUtils.intArray;
import static org.opentripplanner.framework.lang.IntUtils.intArrayToString;
import static org.opentripplanner.framework.lang.IntUtils.intToString;
import static org.opentripplanner.framework.lang.IntUtils.requireInRange;
import static org.opentripplanner.framework.lang.IntUtils.requireNotNegative;
import static org.opentripplanner.framework.lang.IntUtils.shiftArray;
import static org.opentripplanner.framework.lang.IntUtils.standardDeviation;

import java.util.List;
import org.junit.jupiter.api.Test;

class IntUtilsTest {

  @Test
  void testIntToString() {
    assertEquals("7", intToString(7, -1));
    assertEquals("", intToString(-1, -1));
  }

  @SuppressWarnings("RedundantArrayCreation")
  @Test
  void testIntToString2() {
    assertEquals("7, -1", intArrayToString(7, -1));
    assertEquals("", intArrayToString(new int[0]));
  }

  @Test
  void testIntArray() {
    assertArrayEquals(new int[] { 5, 5, 5 }, intArray(3, 5));
  }

  @Test
  public void testRound() {
    assertEquals(0, IntUtils.round(0.499));
    assertEquals(1, IntUtils.round(0.5));
    assertEquals(99, IntUtils.round(99.499));
    assertEquals(100, IntUtils.round(99.5));
    assertEquals(0, IntUtils.round(-0.5));
    assertEquals(-99, IntUtils.round(-99.500));
    assertEquals(-100, IntUtils.round(-99.501));
  }

  @Test
  void testShiftArray() {
    assertArrayEquals(new int[] {}, shiftArray(3, new int[] {}));
    assertArrayEquals(new int[] { 2, 6 }, shiftArray(3, new int[] { -1, 3 }));
    assertArrayEquals(new int[] { -5, -1 }, shiftArray(-4, new int[] { -1, 3 }));
  }

  @Test
  void testConcat() {
    assertArrayEquals(new int[] { 1, 2, 5, 6 }, concat(List.of(1, 2), List.of(5, 6)));
  }

  @Test
  void sestStandardDeviation() {
    assertEquals(0.0, standardDeviation(List.of(7)));
    assertEquals(2.0, standardDeviation(List.of(9, 2, 4, 4, 5, 4, 7, 5)), 0.01);
  }

  @Test
  void testRequireNotNegative() {
    // OK
    assertEquals(7, requireNotNegative(7));
    assertEquals(0, requireNotNegative(0));

    var ex = assertThrows(IllegalArgumentException.class, () -> requireNotNegative(-1));
    assertEquals("Negative value not expected: -1", ex.getMessage());
  }

  @Test
  void testRequireInRange() {
    // OK
    assertEquals(5, requireInRange(5, 5, 5));

    // Too small
    assertThrows(IllegalArgumentException.class, () -> requireInRange(5, 6, 10));
    // Too big
    var ex = assertThrows(IllegalArgumentException.class, () -> requireInRange(5, 1, 4, "cost"));
    assertEquals("The cost is not in range[1, 4]: 5", ex.getMessage());
  }
}
