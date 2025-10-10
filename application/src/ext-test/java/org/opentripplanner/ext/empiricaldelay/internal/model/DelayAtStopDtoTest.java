package org.opentripplanner.ext.empiricaldelay.internal.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class DelayAtStopDtoTest {

  private static final int SEQUENCE = 17;
  private static final FeedScopedId STOP_ID = new FeedScopedId("F", "1");
  private static final EmpiricalDelay EMPIRICAL_DELAY = new EmpiricalDelay(12, 90);

  private final DelayAtStopDto subject = new DelayAtStopDto(SEQUENCE, STOP_ID, EMPIRICAL_DELAY);

  @Test
  void sequence() {
    assertEquals(SEQUENCE, subject.sequence());
  }

  @Test
  void stopId() {
    assertEquals(STOP_ID, subject.stopId());
  }

  @Test
  void empiricalDelay() {
    assertEquals(EMPIRICAL_DELAY, subject.empiricalDelay());
  }
}
