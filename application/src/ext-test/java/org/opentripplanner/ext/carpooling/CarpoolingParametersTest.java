package org.opentripplanner.ext.carpooling;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CarpoolingParametersTest {

  @Test
  void negativeBoardCostIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new CarpoolingParameters(-1));
  }
}
