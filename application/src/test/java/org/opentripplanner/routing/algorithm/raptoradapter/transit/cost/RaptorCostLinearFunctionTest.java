package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.test.support.TestTableParser;

class RaptorCostLinearFunctionTest {

  private static final double COEFFICIENT = 2.5;
  private static final int CONSTANT_SECONDS = 17;

  private final CostLinearFunction sameInOtpDomain = CostLinearFunction.of(
    Cost.costOfSeconds(CONSTANT_SECONDS),
    COEFFICIENT
  );

  private final RaptorCostLinearFunction subject = RaptorCostLinearFunction.of(
    CONSTANT_SECONDS,
    COEFFICIENT
  );

  static Stream<Arguments> calculateRaptorCostTestCases() {
    return TestTableParser.of(
      """
      # expectedRaptorCost | timeSeconds
        1700               | 0
        1950               | 1
        2200               | 2
        5450               | 15
        5700               | 16
       16950               | 61
      """
    );
  }

  @ParameterizedTest
  @MethodSource("calculateRaptorCostTestCases")
  void calculateRaptorCost(int expectedRaptorCost, int timeSeconds) {
    int result = subject.calculateRaptorCost(timeSeconds);
    assertEquals(expectedRaptorCost, result);

    // Make sure we get the same result with the OTP domain function
    assertEquals(
      sameInOtpDomain.calculate(Cost.costOfSeconds(timeSeconds)).toCentiSeconds(),
      result
    );
  }

  @Test
  void isZero() {
    assertFalse(subject.isZero());
    assertTrue(RaptorCostLinearFunction.ZERO_FUNCTION.isZero());
  }

  @Test
  void testEqualsAndHashCode() {
    var same = RaptorCostLinearFunction.of(CONSTANT_SECONDS, COEFFICIENT);
    var otherCoefficient = RaptorCostLinearFunction.of(CONSTANT_SECONDS, 1.2);
    var otherConstant = RaptorCostLinearFunction.of(CONSTANT_SECONDS, 1.2);

    // Equals
    assertEquals(subject, subject);
    assertEquals(same, subject);
    assertNotEquals(null, subject);
    assertNotEquals(1L, subject);
    assertNotEquals(otherCoefficient, subject);
    assertNotEquals(otherConstant, subject);

    // HashCode
    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(otherCoefficient.hashCode(), subject.hashCode());
    assertNotEquals(otherConstant.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("17s + 2.5 t", subject.toString());
    assertEquals("ZERO FUNCTION", RaptorCostLinearFunction.ZERO_FUNCTION.toString());
  }
}
