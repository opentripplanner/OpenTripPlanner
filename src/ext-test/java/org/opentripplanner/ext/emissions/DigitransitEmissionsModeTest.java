package org.opentripplanner.ext.emissions;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DigitransitEmissionsModeTest {

  @Test
  void EmissionsModeWithNormalAvg() {
    DigitransitEmissionsMode mode = new DigitransitEmissionsMode("BUS", "120", 12);
    assertEquals(120.0, mode.getAvg());
  }

  @Test
  void EmissionsModeWithIncorrectAvg() {
    DigitransitEmissionsMode mode = new DigitransitEmissionsMode("BUS", "Foo", 12);
    assertEquals(0, mode.getAvg());
  }
}
