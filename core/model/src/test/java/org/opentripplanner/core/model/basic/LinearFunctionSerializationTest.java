package org.opentripplanner.core.model.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.utils.time.DurationUtils;

class LinearFunctionSerializationTest {

  /**
   * Each case is: input, expected constant, expected normalized coefficient and the expected
   * serialized coefficient. The rows cover each bucket of the coefficient scale.
   */
  @SuppressWarnings("unused")
  static List<Arguments> normalizeAndSerializeTestCases() {
    return List.of(
      Arguments.of("0+0t", "0s", 0.0, "0s + 0.0 t"),
      Arguments.of("1+0.0004 t", "1s", 0.0, "1s + 0.0 t"),
      Arguments.of("1+0.0111 t", "1s", 0.011, "1s + 0.011 t"),
      Arguments.of("1+0.0116 t", "1s", 0.012, "1s + 0.012 t"),
      Arguments.of("1+0.0201 t", "1s", 0.02, "1s + 0.02 t"),
      Arguments.of("1+0.0301 t", "1s", 0.03, "1s + 0.03 t"),
      Arguments.of("1+0.0731 t", "1s", 0.075, "1s + 0.075 t"),
      Arguments.of("120 + 0.111 t", "2m", 0.11, "2m + 0.11 t"),
      Arguments.of("120 + 0.126 t", "2m", 0.13, "2m + 0.13 t"),
      Arguments.of("1 + 0.3 t", "1s", 0.3, "1s + 0.3 t"),
      Arguments.of("1 + 0.73 t", "1s", 0.75, "1s + 0.75 t"),
      Arguments.of("2h3m + 1.111 t", "2h3m", 1.1, "2h3m + 1.1 t"),
      Arguments.of("2h3m + 2.111 t", "2h3m", 2.2, "2h3m + 2.2 t"),
      Arguments.of("3h + 5.111 t", "3h", 5.0, "3h + 5.0 t"),
      Arguments.of("12.0 + 12.4 t", "12s", 12.0, "12s + 12 t"),
      Arguments.of("7m + 10.1 x", "7m", 10.0, "7m + 10 t"),
      Arguments.of("PT7s + 10.1 x", "7s", 10.0, "7s + 10 t"),
      Arguments.of("0.1 + 10.1 x", "0s", 10.0, "0s + 10 t"),
      Arguments.of("0.5 + 10.1 x", "1s", 10.0, "1s + 10 t"),
      Arguments.of("1h + 234 x", "1h", 230.0, "1h + 230 t")
    );
  }

  @ParameterizedTest
  @MethodSource("normalizeAndSerializeTestCases")
  void normalizeAndSerialize(
    String input,
    String expectedConstant,
    double expectedCoefficient,
    String expectedSerialized
  ) {
    var f = LinearFunctionSerialization.parse(input, MyTestLinearFunction::new).orElseThrow();
    assertEquals(DurationUtils.duration(expectedConstant), f.constant);
    assertEquals(expectedCoefficient, f.coefficient);
    assertEquals(expectedSerialized, f.toString());

    // The serialized form is parsed back to the same function
    assertEquals(
      f,
      LinearFunctionSerialization.parse(expectedSerialized, MyTestLinearFunction::new).orElseThrow()
    );
  }

  @Test
  void parseEmpty() {
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse(null, fail()));
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse("", fail()));
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse(" \r\n", fail()));
  }

  @Test
  void serializeUsesTheDecimalsOfTheStepInTheScale() {
    var scale = new double[][] { { 1.0, 0.25 }, { 10.0, 0.5 }, { 100.0, 5.0 }, { 1000.0, 0.001 } };
    var d = Duration.ofSeconds(60);

    // Step 0.25 gives 2 decimals, but trailing zeros are removed down to one decimal
    assertEquals("1m + 0.0 t", LinearFunctionSerialization.serialize(d, 0.0, scale));
    assertEquals("1m + 0.25 t", LinearFunctionSerialization.serialize(d, 0.25, scale));
    assertEquals("1m + 0.5 t", LinearFunctionSerialization.serialize(d, 0.5, scale));
    // Step 0.5 gives 1 decimal
    assertEquals("1m + 1.0 t", LinearFunctionSerialization.serialize(d, 1.0, scale));
    assertEquals("1m + 9.5 t", LinearFunctionSerialization.serialize(d, 9.5, scale));
    // Step 5.0 gives a whole number
    assertEquals("1m + 10 t", LinearFunctionSerialization.serialize(d, 10.0, scale));
    assertEquals("1m + 95 t", LinearFunctionSerialization.serialize(d, 95.0, scale));
    // Step 0.001 gives 3 decimals
    assertEquals("1m + 100.001 t", LinearFunctionSerialization.serialize(d, 100.001, scale));
    assertEquals("1m + 100.0 t", LinearFunctionSerialization.serialize(d, 100.0, scale));
  }

  @Test
  void parseIllegalArgument() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      LinearFunctionSerialization.parse("foo", fail())
    );
    assertEquals("Unable to parse function: 'foo'", ex.getMessage());
  }

  @Test
  void parseIllegalDuration() {
    var ex = assertThrows(IllegalArgumentException.class, () ->
      LinearFunctionSerialization.parse("600ss + 1.3 t", fail())
    );
    assertEquals("Unable to parse duration: '600ss'", ex.getMessage());
  }

  private static BiFunction<Duration, Double, ?> fail() {
    return (a, b) -> Assertions.fail("Factory method called, not expected!");
  }

  private record MyTestLinearFunction(Duration constant, double coefficient) {
    @Override
    public String toString() {
      return LinearFunctionSerialization.serialize(constant, coefficient);
    }
  }
}
