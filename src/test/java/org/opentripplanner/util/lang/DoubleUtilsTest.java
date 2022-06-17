package org.opentripplanner.util.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class DoubleUtilsTest {

  @Test
  void roundTo2Decimals() {
    assertEquals(0.12, DoubleUtils.roundTo2Decimals(0.124999999999));
    assertEquals(0.13, DoubleUtils.roundTo2Decimals(0.125000000001));
    assertEquals(0.12, DoubleUtils.roundTo2Decimals(Double.valueOf(0.124999999999)));
    assertEquals(0.13, DoubleUtils.roundTo2Decimals(Double.valueOf(0.125000000001)));
    assertNull(DoubleUtils.roundTo2Decimals(null));
    assertEquals(DoubleUtils.roundTo2Decimals(Double.NaN), Double.NaN);
    assertEquals(DoubleUtils.roundTo2Decimals(Double.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY);
    assertEquals(DoubleUtils.roundTo2Decimals(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY);
  }

  @Test
  void roundTo7Decimals() {
    assertEquals(0.1234567, DoubleUtils.roundTo7Decimals(0.12345674999));
    assertEquals(0.1234567, DoubleUtils.roundTo7Decimals(0.12345665001));
    assertEquals(0.1234567, DoubleUtils.roundTo7Decimals(Double.valueOf(0.12345674999)));
    assertEquals(0.1234567, DoubleUtils.roundTo7Decimals(Double.valueOf(0.12345665001)));
    assertNull(DoubleUtils.roundTo7Decimals(null));
    assertEquals(DoubleUtils.roundTo7Decimals(Double.NaN), Double.NaN);
    assertEquals(DoubleUtils.roundTo7Decimals(Double.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY);
    assertEquals(DoubleUtils.roundTo7Decimals(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY);
  }
}
