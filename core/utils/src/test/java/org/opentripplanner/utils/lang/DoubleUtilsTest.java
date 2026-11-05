package org.opentripplanner.utils.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.utils.lang.DoubleUtils.requireInRange;
import static org.opentripplanner.utils.lang.DoubleUtils.roundTo1Decimal;
import static org.opentripplanner.utils.lang.DoubleUtils.roundTo2Decimals;
import static org.opentripplanner.utils.lang.DoubleUtils.roundTo3Decimals;
import static org.opentripplanner.utils.lang.DoubleUtils.roundTo4Decimals;
import static org.opentripplanner.utils.lang.DoubleUtils.roundToZeroDecimals;

import org.junit.jupiter.api.Test;

class DoubleUtilsTest {

  @Test
  void testRoundToZeroDecimals() {
    assertEquals(3, roundToZeroDecimals(3.4999999999));
    assertEquals(4, roundToZeroDecimals(3.50));
    assertEquals(5, roundToZeroDecimals(4.5));
  }

  @Test
  void testRoundTo1Decimal() {
    assertEquals(0.3, roundTo1Decimal(0.34999999999));
    assertEquals(0.4, roundTo1Decimal(0.35));
    assertEquals(0.5, roundTo1Decimal(0.45));
  }

  @Test
  void testRoundTo2Decimals() {
    assertEquals(0.12, roundTo2Decimals(0.124999999999));
    assertEquals(0.13, roundTo2Decimals(0.125));
    assertEquals(0.12, roundTo2Decimals(Double.valueOf(0.124999999999)));
    assertEquals(0.13, roundTo2Decimals(Double.valueOf(0.125)));
    assertNull(roundTo2Decimals(null));
  }

  @Test
  void testRoundTo3Decimals() {
    assertEquals(0.712, roundTo3Decimals(0.7124999999999));
    assertEquals(0.713, roundTo3Decimals(0.7125));
  }

  @Test
  void testRoundTo4Decimals() {
    assertEquals(0.7125, roundTo4Decimals(0.7124999999999));
    assertEquals(0.7125, roundTo4Decimals(0.7125));
    assertEquals(0.7126, roundTo4Decimals(0.71255));
    assertEquals(0.7125, roundTo4Decimals(0.71254));
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
    assertEquals(roundTo1Decimal(Double.NaN), Double.NaN);
    assertEquals(roundTo2Decimals(Double.NEGATIVE_INFINITY), Double.NEGATIVE_INFINITY);
    assertEquals(roundTo3Decimals(Double.POSITIVE_INFINITY), Double.POSITIVE_INFINITY);
  }

  @Test
  void testAssertInRange() {
    requireInRange(7.0, 0d, 10d);
    requireInRange(0.0, 0d, 10d);
    requireInRange(10.0, 0d, 10d);

    assertThrows(IllegalArgumentException.class, () -> requireInRange(10.1, 0d, 10d));
    var ex = assertThrows(IllegalArgumentException.class, () -> requireInRange(-0.1, 0d, 10d, "t"));
    assertEquals("The t is not in range[0.0, 10.0]: -0.1", ex.getMessage());
  }

  @Test
  void testEquals() {
    assertTrue(DoubleUtils.doubleEquals(1.2, 1.2));
    assertTrue(DoubleUtils.doubleEquals(-1.2, -1.2));
  }

  /** The buckets: [0, 1)->0.1, [1, 10)->1.0, [10, 100)->10.0, and 10.0 from 100 and up. */
  private static final double[][] STEPS = { { 1.0, 0.1 }, { 10.0, 1.0 }, { 100.0, 10.0 } };

  @Test
  void roundToStepUsesTheStepOfTheBucketOfTheValue() {
    assertEquals(0.4, DoubleUtils.roundToStep(0.37, STEPS));
    assertEquals(3.0, DoubleUtils.roundToStep(3.4, STEPS));
    assertEquals(10.0, DoubleUtils.roundToStep(9.99, STEPS));
    assertEquals(10.0, DoubleUtils.roundToStep(14.0, STEPS));
    assertEquals(20.0, DoubleUtils.roundToStep(16.0, STEPS));
  }

  @Test
  void roundToStepPutsAValueEqualToALimitInTheNextBucket() {
    // 1.0 is in [1, 10) with the step 1.0 - not in [0, 1) with the step 0.1
    assertEquals(1.0, DoubleUtils.roundToStep(1.0, STEPS));
    assertEquals(3.0, DoubleUtils.roundToStep(2.95, STEPS));
    // The same value rounded with the step 0.1 would be 2.9
    assertEquals(0.9, DoubleUtils.roundToStep(0.94, STEPS));
  }

  @Test
  void roundToStepUsesTheLastStepFromTheLastLimitAndUp() {
    assertEquals(100.0, DoubleUtils.roundToStep(100.0, STEPS));
    assertEquals(1230.0, DoubleUtils.roundToStep(1234.0, STEPS));
  }

  @Test
  void roundToStepBreaksTiesTowardsTheEvenNeighbour() {
    // 0.25 / 0.1 = 2.5, a tie - rounds to 2, not 3
    assertEquals(0.2, DoubleUtils.roundToStep(0.25, STEPS));
    // 0.35 / 0.1 = 3.5, a tie - rounds to 4, not 3
    assertEquals(0.4, DoubleUtils.roundToStep(0.35, STEPS));
  }

  @Test
  void roundToStepKeepsZeroAndTheSpecialValues() {
    assertEquals(0.0, DoubleUtils.roundToStep(0.0, STEPS));
    assertEquals(Double.NaN, DoubleUtils.roundToStep(Double.NaN, STEPS));
    assertEquals(
      Double.POSITIVE_INFINITY,
      DoubleUtils.roundToStep(Double.POSITIVE_INFINITY, STEPS)
    );
  }

  @Test
  void roundToStepRejectsAnEmptyTable() {
    assertThrows(IllegalArgumentException.class, () ->
      DoubleUtils.roundToStep(1.0, new double[][] {})
    );
  }
}
