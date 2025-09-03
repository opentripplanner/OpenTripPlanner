package org.opentripplanner.ext.empiricaldelay.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TripDelaysTest {

  private static final FeedScopedId TRIP_ID = new FeedScopedId("F", "Trip-A");
  private static final String WEEKEND = "WEEKEND";
  private static final EmpiricalDelay DELAY_STOP_A = new EmpiricalDelay(25, 50);
  private static final EmpiricalDelay DELAY_STOP_B = new EmpiricalDelay(7, 45);
  private static final int STOP_POS_A = 0;
  private static final int STOP_POS_B = 1;
  private final TripDelays subject = TripDelays.of(TRIP_ID)
    .with(WEEKEND, List.of(DELAY_STOP_A, DELAY_STOP_B))
    .build();

  @Test
  void tripId() {
    assertEquals(TRIP_ID, subject.tripId());
  }

  @Test
  void putAndGet() {
    assertEquals(DELAY_STOP_A, subject.get(WEEKEND, STOP_POS_A).get());
    assertEquals(DELAY_STOP_B, subject.get(WEEKEND, STOP_POS_B).get());
  }

  /**
   * A missing service-id may happen if there is no empirical delay data on a given
   * day of week, but there exist data for another day. Note! There is typical a
   * delay calendar-service for each day-of-week.
   */
  @Test
  void missingServiceIdShouldReturnEmpty() {
    assertEquals(Optional.empty(), subject.get("MONDAY", STOP_POS_A));
  }

  @Test
  void testStopPosOutOfBounds() {
    assertTrue(subject.get(WEEKEND, -1).isEmpty());
    assertTrue(subject.get(WEEKEND, 2).isEmpty());
  }
}
