package org.opentripplanner.utils.lang;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.utils.lang.IntUtils.concat;
import static org.opentripplanner.utils.lang.IntUtils.intArray;
import static org.opentripplanner.utils.lang.IntUtils.intArrayToString;
import static org.opentripplanner.utils.lang.IntUtils.intToString;
import static org.opentripplanner.utils.lang.IntUtils.requireInRange;
import static org.opentripplanner.utils.lang.IntUtils.requireNotNegative;
import static org.opentripplanner.utils.lang.IntUtils.requireNullOrNotNegative;
import static org.opentripplanner.utils.lang.IntUtils.shiftArray;
import static org.opentripplanner.utils.lang.IntUtils.standardDeviation;

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
  void testAssertInRange() {
    IntUtils.requireInRange(1, 1, 1, "single-element-range");
    IntUtils.requireInRange(-2, -2, 1, "negative-start");
    IntUtils.requireInRange(-1, -2, -1, "negative-end");
    assertThrows(IllegalArgumentException.class, () ->
      IntUtils.requireInRange(1, 2, 1, "invalid-range")
    );
    var ex = assertThrows(IllegalArgumentException.class, () ->
      IntUtils.requireInRange(1, 2, 3, "value-too-small")
    );
    assertEquals("The 'value-too-small' is not in range[2, 3]: 1", ex.getMessage());
    ex = assertThrows(IllegalArgumentException.class, () ->
      IntUtils.requireInRange(4, 0, 3, "value-too-big")
    );
    assertEquals("The 'value-too-big' is not in range[0, 3]: 4", ex.getMessage());
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
    assertEquals(7, requireNotNegative(7, "ok"));
    assertEquals(0, requireNotNegative(0, "ok"));

    var ex = assertThrows(IllegalArgumentException.class, () ->
      requireNotNegative(-1, "too-small")
    );
    assertEquals("Negative value not expected for 'too-small': -1", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> requireNotNegative(-1));
    assertEquals("Negative value not expected for value: -1", ex.getMessage());
  }

  @Test
  void testRequireNotNegativeOrNull() {
    assertNull(requireNullOrNotNegative(null, "ok"));
    assertEquals(5, requireNullOrNotNegative(5, "ok"));
    assertEquals(0, requireNullOrNotNegative(0, "ok"));
    assertThrows(IllegalArgumentException.class, () -> requireNullOrNotNegative(-5, "ok"));
  }

  @Test
  void testRequireInRange() {
    // OK
    assertEquals(5, requireInRange(5, 5, 5));

    // Too small
    var ex = assertThrows(IllegalArgumentException.class, () -> requireInRange(5, 6, 10));
    assertEquals("The value is not in range[6, 10]: 5", ex.getMessage());

    // Too big
    ex = assertThrows(IllegalArgumentException.class, () -> requireInRange(5, 1, 4, "cost"));
    assertEquals("The 'cost' is not in range[1, 4]: 5", ex.getMessage());
  }
}
