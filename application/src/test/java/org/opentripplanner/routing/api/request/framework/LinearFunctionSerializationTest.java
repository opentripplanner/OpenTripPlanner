package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.test.support.TestTableParser;
import org.opentripplanner.utils.time.DurationUtils;

class LinearFunctionSerializationTest {

  private static final Duration D2h9s = Duration.ofSeconds(129);
  private static final Duration D1h = Duration.ofSeconds(3600);

  @SuppressWarnings("unused")
  static Stream<Arguments> parseTestCases() {
    return TestTableParser.of(
      """
      #        INPUT    ||       EXPECTED
      #                 ||  CONSTANT | COEFFICIENT
                 0+0t   ||       0s  |   0.0
           1+0.0111 t   ||       1s  |   0.01
        120 + 0.111 t   ||       2m  |   0.11
        120 + 0.111 t   ||       2m  |   0.11
           12.0 + 0 t   ||      12s  |   0.0
       2h3m + 1.111 t   ||     2h3m  |   1.11
       2h3m + 2.111 t   ||     2h3m  |   2.1
         3h + 5.111 t   ||       3h  |   5.1
          7m + 10.1 x   ||       7m  |  10.0
        PT7s + 10.1 x   ||       7s  |  10.0
        0.1 + 10.1 x   ||       0s  |  10.0
        0.5 + 10.1 x   ||       1s  |  10.0
      """
    );
  }

  @ParameterizedTest
  @MethodSource("parseTestCases")
  void parseTest(String input, String expectedConstant, double expectedCoefficient) {
    Optional<MyTestLinearFunction> result = LinearFunctionSerialization.parse(
      input,
      MyTestLinearFunction::new
    );
    var f = result.orElseThrow();
    assertEquals(DurationUtils.duration(expectedConstant), f.constant);
    assertEquals(expectedCoefficient, f.coefficient);
  }

  @Test
  void parseEmpty() {
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse(null, fail()));
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse("", fail()));
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse(" \r\n", fail()));
  }

  @Test
  void serialize() {
    assertEquals("0s + 0.00 t", LinearFunctionSerialization.serialize(Duration.ZERO, 0));
    assertEquals("2m9s + 0.01 t", LinearFunctionSerialization.serialize(D2h9s, 0.0111));
    assertEquals("1h + 0.11 t", LinearFunctionSerialization.serialize(D1h, 0.111));
    assertEquals("1h + 1.11 t", LinearFunctionSerialization.serialize(D1h, 1.111));
    assertEquals("1h + 2.1 t", LinearFunctionSerialization.serialize(D1h, 2.111));
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
