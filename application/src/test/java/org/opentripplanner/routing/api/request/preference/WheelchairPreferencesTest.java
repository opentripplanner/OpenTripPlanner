package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WheelchairPreferencesTest {

  @Test
  void safetyFactorIsKeptWhenInRange() {
    assertEquals(0.4, WheelchairPreferences.of().withSafetyFactor(0.4).build().safetyFactor());
  }

  @ParameterizedTest
  @ValueSource(doubles = { -0.1, 1.1 })
  void safetyFactorOutsideTheRangeIsRejected(double value) {
    var builder = WheelchairPreferences.of().withSafetyFactor(value);
    assertThrows(IllegalArgumentException.class, builder::build);
  }
}
