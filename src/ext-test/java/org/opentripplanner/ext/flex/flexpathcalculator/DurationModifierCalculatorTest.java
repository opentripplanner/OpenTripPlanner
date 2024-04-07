package org.opentripplanner.ext.flex.flexpathcalculator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.geometry.LineStrings;
import org.opentripplanner.ext.flex.trip.DurationModifier;
import org.opentripplanner.street.model._data.StreetModelForTest;

class DurationModifierCalculatorTest {

  private static final int THIRTY_MINS_IN_SECONDS = (int) Duration.ofMinutes(30).toSeconds();

  @Test
  void calculate() {
    FlexPathCalculator delegate = (fromv, tov, fromStopIndex, toStopIndex) ->
      new FlexPath(10_000, THIRTY_MINS_IN_SECONDS, () -> LineStrings.SIMPLE);

    var mod = new DurationModifier(Duration.ofMinutes(10), 1.5f);
    var calc = new DurationModifierCalculator(delegate, mod);
    var path = calc.calculateFlexPath(StreetModelForTest.V1, StreetModelForTest.V2, 0, 5);
    assertEquals(3300, path.durationSeconds);
  }

  @Test
  void nullValue() {
    FlexPathCalculator delegate = (fromv, tov, fromStopIndex, toStopIndex) -> null;
    var mod = new DurationModifier(Duration.ofMinutes(10), 1.5f);
    var calc = new DurationModifierCalculator(delegate, mod);
    var path = calc.calculateFlexPath(StreetModelForTest.V1, StreetModelForTest.V2, 0, 5);
    assertNull(path);
  }
}
