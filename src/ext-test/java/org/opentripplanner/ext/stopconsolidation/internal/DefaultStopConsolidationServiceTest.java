package org.opentripplanner.ext.stopconsolidation.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.service.TransitModel;

class DefaultStopConsolidationServiceTest {

  @Test
  void isActive() {
    var service = new DefaultStopConsolidationService(
      new DefaultStopConsolidationRepository(),
      new TransitModel()
    );
    assertFalse(service.isActive());
  }
}
