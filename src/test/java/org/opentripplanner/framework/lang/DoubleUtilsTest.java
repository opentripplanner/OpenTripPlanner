package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.lang.DoubleUtils.assertInRange;

import org.junit.jupiter.api.Test;

class DoubleUtilsTest {

  @Test
  void roundToZeroDecimals() {
    assertEquals(3, DoubleUtils.roundToZeroDecimals(3.4999999999));
    assertEquals(4, DoubleUtils.roundToZeroDecimals(3.50));
    assertEquals(5, DoubleUtils.roundToZeroDecimals(4.5));
  }

  @Test
  void roundTo1Decimal() {
    assertEquals(0.3, DoubleUtils.roundTo1Decimal(0.34999999999));
    assertEquals(0.4, DoubleUtils.roundTo1Decimal(0.35));
    assertEquals(0.5, DoubleUtils.roundTo1Decimal(0.45));
  }

  @Test
  void roundTo2Decimals() {
    assertEquals(0.12, DoubleUtils.roundTo2Decimals(0.124999999999));
    assertEquals(0.13, DoubleUtils.roundTo2Decimals(0.125));
    assertEquals(0.12, DoubleUtils.roundTo2Decimals(Double.valueOf(0.124999999999)));
    assertEquals(0.13, DoubleUtils.roundTo2Decimals(Double.valueOf(0.125)));
    assertNull(DoubleUtils.roundTo2Decimals(null));
  }

  @Test
  void roundTo3Decimals() {
    assertEquals(0.712, DoubleUtils.roundTo3Decimals(0.7124999999999));
    assertEquals(0.713, DoubleUtils.roundTo3Decimals(0.7125));
  }

  @Test
  void roundTo7Decimals() {
    assertEquals(0.1234567, DoubleUtils.roundTo7Decimals(0.12345674999));
    assertEquals(0.1234567, DoubleUtils.roundTo7Decimals(0.12345665));
    assertEquals(0.1234567, DoubleUtils.roundTo7Decimals(Double.valueOf(0.12345674999)));
    assertEquals(0.1234567, DoubleUtils.roundTo7Decimals(Double.valueOf(0.12345665)));
    assertNull(DoubleUtils.roundTo7Decimals(null));
  }

  @Test
  void specialCases() {
    assertEquals(DoubleUtils.roundTo1Decimal(Double.NaN), Double.NaN);
    assertEquals(DoubleUtils.roundTo2Decimals(Double.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY);
    assertEquals(DoubleUtils.roundTo3Decimals(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY);
  }

  @Test
  void testAssertInRange() {
    assertInRange(7.0, 0d, 10d, "test");
    assertInRange(0.0, 0d, 10d, "test");
    assertInRange(10.0, 0d, 10d, "test");

    assertThrows(IllegalArgumentException.class, () -> assertInRange(10.1, 0d, 10d, "test"), "");
    assertThrows(IllegalArgumentException.class, () -> assertInRange(-0.1, 0d, 10d, "test"), "");
  }

  @Test
  void testEquals() {
    assertTrue(DoubleUtils.doubleEquals(1.2, 1.2));
    assertTrue(DoubleUtils.doubleEquals(-1.2, -1.2));
  }
}
