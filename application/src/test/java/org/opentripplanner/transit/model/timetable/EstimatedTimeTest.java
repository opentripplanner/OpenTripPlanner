package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class EstimatedTimeTest {

  private static final ZonedDateTime SCHEDULED = ZonedDateTime.of(
    2025,
    1,
    15,
    10,
    0,
    0,
    0,
    ZoneId.of("Europe/Helsinki")
  );

  @Test
  void delayedVehicleIsLaterThanScheduled() {
    var estimate = EstimatedTime.of(SCHEDULED, 120);
    assertEquals(SCHEDULED.plusMinutes(2), estimate.time());
    assertEquals(Duration.ofMinutes(2), estimate.delay());
  }

  @Test
  void earlyVehicleIsEarlierThanScheduled() {
    var estimate = EstimatedTime.of(SCHEDULED, -60);
    assertEquals(SCHEDULED.minusMinutes(1), estimate.time());
    assertEquals(Duration.ofMinutes(-1), estimate.delay());
  }

  @Test
  void onTimeVehicle() {
    var estimate = EstimatedTime.of(SCHEDULED, 0);
    assertEquals(SCHEDULED, estimate.time());
    assertEquals(Duration.ZERO, estimate.delay());
  }
}
