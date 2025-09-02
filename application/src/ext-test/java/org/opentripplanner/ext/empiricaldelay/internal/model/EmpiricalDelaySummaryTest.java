package org.opentripplanner.ext.empiricaldelay.internal.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class EmpiricalDelaySummaryTest {

  @Test
  void summary() {
    var summary = new EmpiricalDelaySummary();
    // An empty summary should not throw an exception
    assertDoesNotThrow(() -> summary.toString());

    for (int i = 1; i <= 2; ++i) {
      String feedId = "F" + i;
      summary.incServiceCalendars(feedId);
      for (int j = 1; j <= 45 * i; ++j) {
        summary.incTrips(new FeedScopedId(feedId, "T" + j));
      }
    }
    assertEquals(
      "Empirical Delay Summary - (Total: 2 | 135), (F1: 1 | 45), (F2: 1 | 90)",
      summary.summary()
    );
    assertEquals(summary.summary(), summary.toString());
  }
}
