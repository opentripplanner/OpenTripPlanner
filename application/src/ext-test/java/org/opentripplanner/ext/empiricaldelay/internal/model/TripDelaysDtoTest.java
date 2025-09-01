package org.opentripplanner.ext.empiricaldelay.internal.model;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.empiricaldelay.model.EmpiricalDelay;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class TripDelaysDtoTest {

  private static final String MON_FRI = "MON-FRI";
  private static final String WEEKEND = "SAT-SUN";
  private static final FeedScopedId TRIP_ID = new FeedScopedId("F", "1");
  private static final FeedScopedId STOP_01 = new FeedScopedId("F", "S1");
  private static final FeedScopedId STOP_02 = new FeedScopedId("F", "S2");
  private static final FeedScopedId STOP_03 = new FeedScopedId("F", "S3");
  private static final EmpiricalDelay DELAY_01 = new EmpiricalDelay(12, 27);
  private static final EmpiricalDelay DELAY_02 = new EmpiricalDelay(17, 57);
  private static final EmpiricalDelay DELAY_03 = new EmpiricalDelay(13, 30);
  private static final EmpiricalDelay DELAY_11 = new EmpiricalDelay(12, 120);
  private static final EmpiricalDelay DELAY_12 = new EmpiricalDelay(17, 97);
  private static final EmpiricalDelay DELAY_13 = new EmpiricalDelay(13, 17);
  private final TripDelaysDto subject = new TripDelaysDto(TRIP_ID);

  @BeforeEach
  void setup() {
    subject.addDelay(MON_FRI, 2, STOP_03, DELAY_03);
    subject.addDelay(MON_FRI, 0, STOP_01, DELAY_01);
    subject.addDelay(MON_FRI, 1, STOP_02, DELAY_02);
    subject.addDelay(WEEKEND, 1, STOP_02, DELAY_12);
    subject.addDelay(WEEKEND, 2, STOP_03, DELAY_13);
    subject.addDelay(WEEKEND, 0, STOP_01, DELAY_11);
  }

  @Test
  void tripId() {
    assertEquals(TRIP_ID, subject.tripId());
  }

  @Test
  void serviceIds() {
    assertThat(subject.serviceIds()).containsExactly(MON_FRI, WEEKEND);
  }

  @Test
  void delaysForServiceId() {
    var mf = subject.delaysSortedForServiceId(MON_FRI);
    assertDelayAtStopDto(mf.get(0), STOP_01, 0, DELAY_01);
    assertDelayAtStopDto(mf.get(1), STOP_02, 1, DELAY_02);
    assertDelayAtStopDto(mf.get(2), STOP_03, 2, DELAY_03);

    var w = subject.delaysSortedForServiceId(WEEKEND);
    assertDelayAtStopDto(w.get(0), STOP_01, 0, DELAY_11);
    assertDelayAtStopDto(w.get(1), STOP_02, 1, DELAY_12);
    assertDelayAtStopDto(w.get(2), STOP_03, 2, DELAY_13);
  }

  void assertDelayAtStopDto(
    DelayAtStopDto delayAtStop,
    FeedScopedId expStopId,
    int expSequence,
    EmpiricalDelay delay
  ) {
    assertEquals(expStopId, delayAtStop.stopId());
    assertEquals(expSequence, delayAtStop.sequence());
    assertEquals(delay, delayAtStop.empiricalDelay());
  }
}
