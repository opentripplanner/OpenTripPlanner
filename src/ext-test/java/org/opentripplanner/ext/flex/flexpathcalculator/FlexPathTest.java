package org.opentripplanner.ext.flex.flexpathcalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner._support.geometry.LineStrings;
import org.opentripplanner.ext.flex.trip.DurationModifier;

class FlexPathTest {

  private static final int THIRTY_MINS_IN_SECONDS = (int) Duration.ofMinutes(30).toSeconds();
  private static final FlexPath PATH = new FlexPath(
    10_000,
    THIRTY_MINS_IN_SECONDS,
    () -> LineStrings.SIMPLE
  );

  static List<Arguments> cases() {
    return List.of(
      Arguments.of(DurationModifier.NONE, THIRTY_MINS_IN_SECONDS),
      Arguments.of(new DurationModifier(Duration.ofMinutes(10), 1), 2400),
      Arguments.of(new DurationModifier(Duration.ofMinutes(10), 1.5f), 3300),
      Arguments.of(new DurationModifier(Duration.ZERO, 3), 5400)
    );
  }

  @ParameterizedTest
  @MethodSource("cases")
  void calculate(DurationModifier mod, int expectedSeconds) {
    var modified = PATH.withDurationModifier(mod);
    assertEquals(expectedSeconds, modified.durationSeconds);
    assertEquals(LineStrings.SIMPLE, modified.getGeometry());
  }
}
