package org.opentripplanner.street.model.elevation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BicycleSlopeSpeedFunctionTest {

  @Test
  void coefficientPositiveAboveUpperSlopeBound() {
    assertTrue(BicycleSlopeSpeedFunction.coefficient(0.5, 0) > 0);
  }

  @Test
  void coefficientPositiveBelowLowerSlopeBound() {
    assertTrue(BicycleSlopeSpeedFunction.coefficient(-0.5, 0) > 0);
  }

  @Test
  void coefficientIsIdempotentAtUpperSlopeBound() {
    assertEquals(
      BicycleSlopeSpeedFunction.coefficient(BicycleSlopeSpeedFunction.MAX_SLOPE, 0),
      BicycleSlopeSpeedFunction.coefficient(0.36, 0)
    );
  }

  @Test
  void coefficientIsIdempotentAtLowerSlopeBound() {
    assertEquals(
      BicycleSlopeSpeedFunction.coefficient(BicycleSlopeSpeedFunction.MIN_SLOPE, 0),
      BicycleSlopeSpeedFunction.coefficient(-0.36, 0)
    );
  }
}
