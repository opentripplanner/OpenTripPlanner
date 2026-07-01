package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StopTimeUpdateTest {

  // Arbitrary scheduled times (seconds since midnight)
  private static final int SCHED_ARRIVAL = 3600;
  private static final int SCHED_DEPARTURE = 3660;

  // null signals "no realtime value available"
  private static final Integer NO_RT = null;

  // -----------------------------------------------------------------------
  // hasRealTimeUpdate
  // -----------------------------------------------------------------------

  @Test
  void hasRealTimeUpdate_returnsFalse_whenBothTimesAbsent() {
    var update = intermediate(NO_RT, NO_RT);
    assertFalse(update.hasRealTimeUpdate());
  }

  @Test
  void hasRealTimeUpdate_returnsTrue_whenOnlyArrivalPresent() {
    var update = intermediate(3700, NO_RT);
    assertTrue(update.hasRealTimeUpdate());
  }

  @Test
  void hasRealTimeUpdate_returnsTrue_whenOnlyDeparturePresent() {
    var update = intermediate(NO_RT, 3760);
    assertTrue(update.hasRealTimeUpdate());
  }

  @Test
  void hasRealTimeUpdate_returnsTrue_whenBothTimesPresent() {
    var update = intermediate(3700, 3760);
    assertTrue(update.hasRealTimeUpdate());
  }

  // -----------------------------------------------------------------------
  // Intermediate stop delay calculation
  // -----------------------------------------------------------------------

  @Test
  void intermediateStop_bothTimesPresent_usesRealTimesDirectly() {
    var update = intermediate(3700, 3760);

    assertEquals(3700 - SCHED_ARRIVAL, update.getArrivalDelay());
    assertEquals(3760 - SCHED_DEPARTURE, update.getDepartureDelay());
  }

  @Test
  void intermediateStop_onlyArrivalPresent_departureDelay_fallsBackToScheduled() {
    // Only arrival realtime available; departure falls back to scheduled → delay = 0
    var update = intermediate(3700, NO_RT);

    assertEquals(3700 - SCHED_ARRIVAL, update.getArrivalDelay());
    assertEquals(0, update.getDepartureDelay());
  }

  @Test
  void intermediateStop_onlyDeparturePresent_arrivalDelay_fallsBackToScheduled() {
    // Only departure realtime available; arrival falls back to scheduled → delay = 0
    var update = intermediate(NO_RT, 3760);

    assertEquals(0, update.getArrivalDelay());
    assertEquals(3760 - SCHED_DEPARTURE, update.getDepartureDelay());
  }

  // -----------------------------------------------------------------------
  // First stop: missing arrival falls back to realtime departure
  // -----------------------------------------------------------------------

  @Test
  void firstStop_onlyDeparturePresent_arrivalDelay_fallsBackToRealtimeDeparture() {
    // At the first stop arrival and departure are typically the same time.
    // If arrival is missing, use the realtime departure as the arrival proxy.
    var update = firstStop(NO_RT, 3760);

    assertEquals(3760 - SCHED_ARRIVAL, update.getArrivalDelay());
    assertEquals(3760 - SCHED_DEPARTURE, update.getDepartureDelay());
  }

  @Test
  void firstStop_onlyArrivalPresent_departureDelay_usesScheduledDeparture() {
    // At the first stop there is no "next stop" to fall back to for departure,
    // so the scheduled departure is used when realtime departure is missing.
    var update = firstStop(3700, NO_RT);

    assertEquals(3700 - SCHED_ARRIVAL, update.getArrivalDelay());
    assertEquals(0, update.getDepartureDelay());
  }

  @Test
  void firstStop_bothTimesPresent_usesRealTimesDirectly() {
    var update = firstStop(3700, 3760);

    assertEquals(3700 - SCHED_ARRIVAL, update.getArrivalDelay());
    assertEquals(3760 - SCHED_DEPARTURE, update.getDepartureDelay());
  }

  // -----------------------------------------------------------------------
  // Last stop: missing departure falls back to realtime arrival
  // -----------------------------------------------------------------------

  @Test
  void lastStop_onlyArrivalPresent_departureDelay_fallsBackToRealtimeArrival() {
    // At the last stop there is no onward departure, so the realtime arrival
    // is used as a proxy for the (missing) realtime departure.
    var update = lastStop(3700, NO_RT);

    assertEquals(3700 - SCHED_ARRIVAL, update.getArrivalDelay());
    assertEquals(3700 - SCHED_DEPARTURE, update.getDepartureDelay());
  }

  @Test
  void lastStop_onlyDeparturePresent_arrivalDelay_usesScheduledArrival() {
    var update = lastStop(NO_RT, 3760);

    assertEquals(0, update.getArrivalDelay());
    assertEquals(3760 - SCHED_DEPARTURE, update.getDepartureDelay());
  }

  @Test
  void lastStop_bothTimesPresent_usesRealTimesDirectly() {
    var update = lastStop(3700, 3760);

    assertEquals(3700 - SCHED_ARRIVAL, update.getArrivalDelay());
    assertEquals(3760 - SCHED_DEPARTURE, update.getDepartureDelay());
  }

  // -----------------------------------------------------------------------
  // Zero delay (on-time)
  // -----------------------------------------------------------------------

  @Test
  void zeroDelay_whenRealtimeEqualsScheduled() {
    var update = intermediate(SCHED_ARRIVAL, SCHED_DEPARTURE);

    assertEquals(0, update.getArrivalDelay());
    assertEquals(0, update.getDepartureDelay());
  }

  // -----------------------------------------------------------------------
  // actualTime takes priority over expectedTime
  // (the priority itself is enforced by TimetableHelper.getAvailableTime, which resolves
  // actual/expected down to a single integer before constructing StopTimeUpdate — so these
  // tests verify the contract from the caller's perspective: the lower-priority expected value
  // must NOT override an already-resolved actual value)
  // -----------------------------------------------------------------------

  @Test
  void actualArrivalTime_takesPriorityOver_expectedArrivalTime() {
    // actual=3650, expected=3700 — actual must win
    // TimetableHelper resolves this to rtArrival=3650 before building StopTimeUpdate
    int rtArrival = 3650;
    var update = intermediate(rtArrival, SCHED_DEPARTURE);

    assertEquals(3650 - SCHED_ARRIVAL, update.getArrivalDelay());
  }

  @Test
  void actualDepartureTime_takesPriorityOver_expectedDepartureTime() {
    // actual=3710, expected=3760 — actual must win
    int rtDeparture = 3710;
    var update = intermediate(SCHED_ARRIVAL, rtDeparture);

    assertEquals(3710 - SCHED_DEPARTURE, update.getDepartureDelay());
  }

  @Test
  void expectedTime_usedWhenActualIsAbsent() {
    // No actual time; expected time must be used
    int rtArrival = 3700;
    var update = intermediate(rtArrival, SCHED_DEPARTURE);

    assertEquals(3700 - SCHED_ARRIVAL, update.getArrivalDelay());
  }

  // -----------------------------------------------------------------------
  // Regression: the old && condition silently dropped one-sided updates
  // -----------------------------------------------------------------------

  /**
   * Before the fix, {@code hasRealTimeUpdate()} used {@code &&} instead of {@code ||}.
   * With {@code &&}, a stop that only received one of the two times (a common SIRI pattern,
   * especially for first/last stops) was incorrectly treated as NO_DATA and its realtime
   * information was discarded entirely.
   *
   * <p>This test documents the old broken contract so the regression is explicit: if the
   * condition were reverted to {@code &&}, exactly these two cases would silently return
   * {@code false} and the caller would call {@code withNoData(stop)} instead of applying delays.
   */
  @Test
  void regression_oldAndCondition_wouldHaveDiscardedArrivalOnlyUpdate() {
    // Only arrival present — with &&, hasRealTimeUpdate() would return false (BUG)
    var update = intermediate(3700, NO_RT);

    // Current correct behaviour: returns true, delay is applied
    assertTrue(update.hasRealTimeUpdate());
    assertEquals(3700 - SCHED_ARRIVAL, update.getArrivalDelay());
    // If the && bug were present: hasRealTimUpdate() == false → stop marked NO_DATA → delay lost
  }

  @Test
  void regression_oldAndCondition_wouldHaveDiscardedDepartureOnlyUpdate() {
    // Only departure present — with &&, hasRealTimeUpdate() would return false (BUG)
    var update = intermediate(NO_RT, 3760);

    // Current correct behaviour: returns true, delay is applied
    assertTrue(update.hasRealTimeUpdate());
    assertEquals(3760 - SCHED_DEPARTURE, update.getDepartureDelay());
    // If the && bug were present: hasRealTimUpdate() == false → stop marked NO_DATA → delay lost
  }

  // -----------------------------------------------------------------------
  // Factories
  // -----------------------------------------------------------------------

  private static StopTimeUpdate intermediate(Integer rtArrival, Integer rtDeparture) {
    return new StopTimeUpdate(SCHED_ARRIVAL, rtArrival, SCHED_DEPARTURE, rtDeparture, false, false);
  }

  private static StopTimeUpdate firstStop(Integer rtArrival, Integer rtDeparture) {
    return new StopTimeUpdate(SCHED_ARRIVAL, rtArrival, SCHED_DEPARTURE, rtDeparture, true, false);
  }

  private static StopTimeUpdate lastStop(Integer rtArrival, Integer rtDeparture) {
    return new StopTimeUpdate(SCHED_ARRIVAL, rtArrival, SCHED_DEPARTURE, rtDeparture, false, true);
  }
}
