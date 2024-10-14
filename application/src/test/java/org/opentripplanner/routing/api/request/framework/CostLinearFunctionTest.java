package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.test.support.TestTableParser;

class CostLinearFunctionTest {

  private static final Duration D2m = Duration.ofMinutes(2);
  private static final Cost COST_1s = Cost.costOfSeconds(1);
  private static final Cost COST_2s = Cost.costOfSeconds(2);
  private static final Cost COST_10s = Cost.costOfSeconds(10);
  private static final Cost COST_11s = Cost.costOfSeconds(11);
  private static final Cost COST_61s = Cost.costOfSeconds(61);

  @Test
  void parse() {
    assertEquals(CostLinearFunction.of(D2m, 3.0), CostLinearFunction.of("2m+3t"));
  }

  @Test
  void parsePenaltyRandomInputNotAllowed() {
    assertThrows(IllegalArgumentException.class, () -> CostLinearFunction.of("xyz"));
  }

  @Test
  void negativeDurationNotAllowed() {
    assertThrows(IllegalArgumentException.class, () -> CostLinearFunction.of("-2m + 1.0 t"));
    assertThrows(
      IllegalArgumentException.class,
      () -> CostLinearFunction.of(DurationUtils.duration("-2m"), 1.0)
    );
  }

  @Test
  void emptyInputNotAllowed() {
    assertThrows(NoSuchElementException.class, () -> CostLinearFunction.of(null));
    var e = assertThrows(NoSuchElementException.class, () -> CostLinearFunction.of(""));
    assertEquals("No value present", e.getMessage());
  }

  @Test
  void parsePenaltyTimeCoefficientMustBeAtLeastZeroAndLessThanTen() {
    assertThrows(IllegalArgumentException.class, () -> CostLinearFunction.of(D2m, -0.01));
    var ex = assertThrows(
      IllegalArgumentException.class,
      () -> CostLinearFunction.of(Duration.ZERO, 100.1)
    );
    assertEquals("The value is not in range[0.0, 100.0]: 100.1", ex.getMessage());
  }

  @Test
  void testToStringIsParsableAndCanBeUsedForSerialization() {
    var original = CostLinearFunction.of(D2m, 1.7);
    var copy = CostLinearFunction.of(original.toString());
    assertEquals(original, copy);
  }

  @Test
  void isZero() {
    assertTrue(CostLinearFunction.ZERO.isZero());
    assertTrue(CostLinearFunction.of("0s + 0t").isZero());
    assertFalse(CostLinearFunction.of("1s + 0t").isZero());
    assertFalse(CostLinearFunction.of("0s + 0.1t").isZero());
  }

  static Stream<Arguments> calculateTestCases() {
    return TestTableParser.of(
      """
      #  function  ||          expected values
      #            ||  0s |  1s |  2s |  10s | 11s | 61s
       0s + 0.0 t  ||   0 |   0 |   0 |   0  |   0 |   0
       7s + 0.0 t  ||   7 |   7 |   7 |   7  |   7 |   7
       0s + 1.0 t  ||   0 |   1 |   2 |  10  |  11 |  61
       0s + 1.05 t ||   0 |   1 |   2 |  11  |  12 |  64
       8s + 2.0 t  ||   8 |  10 |  12 |  28  |  30 | 130
       8s + 2.04 t ||   8 |  10 |  12 |  28  |  30 | 130
      """
    );
  }

  @ParameterizedTest
  @MethodSource("calculateTestCases")
  void calculate(
    String function,
    int exp0s,
    int exp1s,
    int exp2s,
    int exp10s,
    int exp11s,
    int exp61s
  ) {
    var subject = CostLinearFunction.of(function);

    assertEquals(exp0s, subject.calculate(Cost.ZERO).toSeconds());
    assertEquals(exp1s, subject.calculate(COST_1s).toSeconds());
    assertEquals(exp2s, subject.calculate(COST_2s).toSeconds());
    assertEquals(exp10s, subject.calculate(COST_10s).toSeconds());
    assertEquals(exp11s, subject.calculate(COST_11s).toSeconds());
    assertEquals(exp61s, subject.calculate(COST_61s).toSeconds());
  }
}
