package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.test.support.VariableSource;

class LinearFunctionSerializationTest {

  private static final Duration D2h9s = Duration.ofSeconds(129);
  private static final Duration D1h = Duration.ofSeconds(3600);

  @SuppressWarnings("unused")
  static Stream<Arguments> parseTestCases = Stream.of(
    Arguments.of("0s + 0.00 t", "0+0t"),
    Arguments.of("1s + 0.01 t", "1+0.0111 t"),
    Arguments.of("2m + 0.11 t", "120 + 0.111 t"),
    Arguments.of("2h3m + 1.11 t", "2h3m + 1.111 t"),
    Arguments.of("3h + 5.1 t", "3h + 5.111 t")
  );

  @ParameterizedTest
  @VariableSource("parseTestCases")
  void parseTest(String expected, String input) {
    Optional<LinearFunction> result = LinearFunctionSerialization.parse(input, LinearFunction::new);
    assertEquals(expected, result.orElseThrow().toString());
  }

  @Test
  void parseEmtpy() {
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse(null, LinearFunction::new));
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse("", LinearFunction::new));
    assertEquals(Optional.empty(), LinearFunctionSerialization.parse(" \r\n", LinearFunction::new));
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
    var ex = assertThrows(
      IllegalArgumentException.class,
      () -> LinearFunctionSerialization.parse("foo", LinearFunction::new)
    );
    assertEquals("Unable to parse function: 'foo'", ex.getMessage());
  }

  private record LinearFunction(Duration a, double b) {
    @Override
    public String toString() {
      return LinearFunctionSerialization.serialize(a, b);
    }
  }
}
