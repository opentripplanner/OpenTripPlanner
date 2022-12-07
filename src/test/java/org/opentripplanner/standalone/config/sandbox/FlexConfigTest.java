package org.opentripplanner.standalone.config.sandbox;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FlexConfigTest {

  @Test
  void initializationOrder() {
    assertNotNull(FlexConfig.DEFAULT.maxTransferDuration());
    assertNotNull(FlexConfig.DEFAULT.maxFlexTripDuration());
  }
}
