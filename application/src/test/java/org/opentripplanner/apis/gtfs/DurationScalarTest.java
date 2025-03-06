package org.opentripplanner.apis.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

import graphql.schema.CoercingSerializeException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class DurationScalarTest {

  static List<Arguments> durationCases() {
    return List.of(
      of(Duration.ofMinutes(30), "PT30M"),
      of(Duration.ofHours(23), "PT23H"),
      of(Duration.ofMinutes(-10), "-PT10M"),
      of(Duration.ofMinutes(-90), "-PT1H30M"),
      of(Duration.ofMinutes(-184), "-PT3H4M")
    );
  }

  @ParameterizedTest
  @MethodSource("durationCases")
  void duration(Duration duration, String expected) {
    var string = GraphQLScalars.DURATION_SCALAR.getCoercing().serialize(duration);
    assertEquals(expected, string);
  }

  @Test
  void nonDuration() {
    Assertions.assertThrows(CoercingSerializeException.class, () ->
      GraphQLScalars.DURATION_SCALAR.getCoercing().serialize(new Object())
    );
  }
}
