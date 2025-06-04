package org.opentripplanner.ext.emission.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class EmissionSummaryTest {

  @Test
  void testToString() {
    var subject = new EmissionSummary(12, 98732);
    assertEquals("Emission Summary - route: 12 / trip: 98,732 / total: 98,744", subject.toString());
  }
}
