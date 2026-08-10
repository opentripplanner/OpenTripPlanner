package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

/**
 * Tests for the handling of SIRI ET updates where real-time times are partially or fully absent
 * for stops in a trip.
 *
 * <p>The key behaviours under test:
 * <ul>
 *   <li>A stop that receives only one of arrival/departure time must NOT be marked NO_DATA.</li>
 *   <li>A stop with no real-time times at all must be marked NO_DATA.</li>
 *   <li>A trip where every stop is NO_DATA must have {@code timesModified = false}, so
 *       {@code hasAnyUpdates()} returns {@code false} and the trip prefix is "S" (scheduled).</li>
 * </ul>
 */
class MissingRealtimeTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TWO_STOP_TRIP = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  private final TripInput THREE_STOP_TRIP = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21")
    .addStop(STOP_C, "0:00:30", "0:00:31");

  /**
   * An intermediate stop that provides only a departure time (no arrival) must be treated as a
   * real-time update, not NO_DATA. The missing arrival falls back to the scheduled arrival
   * (delay = 0), while the departure carries the provided delay.
   */
  @Test
  void intermediateStop_withOnlyDepartureTime_isNotNoData() {
    var env = ENV_BUILDER.addTrip(THREE_STOP_TRIP).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          // departure only – arrival falls back to scheduled
          .departAimedExpected("00:00:21", "00:00:26")
          .call(STOP_C)
          .arriveAimedExpected("00:00:30", "00:00:35")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);

    // Stop B: arrival = scheduled (0:00:20, delay 0), departure = real-time (0:00:26, +5s)
    assertEquals(
      "U | A 0:00:15 0:00:15 | B 0:00:20 0:00:26 | C 0:00:35 0:00:35",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * An intermediate stop that provides only an arrival time (no departure) must be treated as a
   * real-time update. The missing departure falls back to the scheduled departure (delay = 0).
   * The real-time arrival must not exceed the scheduled departure to avoid a negative dwell time.
   */
  @Test
  void intermediateStop_withOnlyArrivalTime_isNotNoData() {
    var env = ENV_BUILDER.addTrip(THREE_STOP_TRIP).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          // arrival only – departure falls back to scheduled
          .arriveAimedExpected("00:00:20", "00:00:20")
          .call(STOP_C)
          .arriveAimedExpected("00:00:30", "00:00:35")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);

    // Stop B: arrival = real-time (0:00:20, delay 0), departure = scheduled (0:00:21, delay 0)
    assertEquals(
      "U | A 0:00:15 0:00:15 | B 0:00:20 0:00:21 | C 0:00:35 0:00:35",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * A stop with no real-time times at all must be marked NO_DATA and display the scheduled times.
   * The other stops in the same trip have valid updates, so the trip prefix is "U" (updated).
   */
  @Test
  void oneStopWithNoTimes_isMarkedNoData_tripStillUpdated() {
    var env = ENV_BUILDER.addTrip(THREE_STOP_TRIP).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          // no times → NO_DATA, scheduled times shown
          .call(STOP_B)
          .call(STOP_C)
          .arriveAimedExpected("00:00:30", "00:00:35")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);

    assertEquals(
      "U | A 0:00:15 0:00:15 | B [ND] 0:00:20 0:00:21 | C 0:00:35 0:00:35",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * When every stop in the update has no real-time times, all stops are marked NO_DATA and
   * {@code timesModified} remains {@code false}. The trip prefix must be "S" (scheduled), not "U"
   * (updated), because no times were actually modified.
   */
  @Test
  void allStopsWithNoTimes_timesModifiedIsFalse_tripAppearsScheduled() {
    var env = ENV_BUILDER.addTrip(TWO_STOP_TRIP).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder -> builder.call(STOP_A).call(STOP_B))
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);

    // "S" prefix: hasAnyUpdates() = false (timesModified = false, no cancellation/added/etc.)
    assertEquals(
      "S | A [ND] 0:00:10 0:00:11 | B [ND] 0:00:20 0:00:21",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * The first stop of a trip typically only provides a departure time (no arrival). This must be
   * treated as a valid update. The missing arrival falls back to the real-time departure time
   * (first-stop rule), so both arrival and departure show the same real-time value.
   */
  @Test
  void firstStop_withOnlyDepartureTime_isNotNoData() {
    var env = ENV_BUILDER.addTrip(TWO_STOP_TRIP).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
          .departAimedExpected("00:00:21", "00:00:26")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);

    // Stop A: missing arrival falls back to real-time departure (0:00:15) → both show 0:00:15
    assertEquals(
      "U | A 0:00:15 0:00:15 | B 0:00:25 0:00:26",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * The last stop of a trip typically only provides an arrival time (no departure). This must be
   * treated as a valid update. The missing departure falls back to the real-time arrival time
   * (last-stop rule), so both arrival and departure show the same real-time value.
   */
  @Test
  void lastStop_withOnlyArrivalTime_isNotNoData() {
    var env = ENV_BUILDER.addTrip(TWO_STOP_TRIP).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);

    // Stop B: missing departure falls back to real-time arrival (0:00:25) → both show 0:00:25
    assertEquals(
      "U | A 0:00:15 0:00:15 | B 0:00:25 0:00:25",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * A journey-level cancellation with no stop times provided must set {@code isCanceled = true}
   * and leave {@code timesModified = false}. The trip is "updated" because it is cancelled
   * (prefix "C U"), but no time modifications were made, so the scheduled times are shown.
   *
   * <p>This verifies that {@code hasAnyUpdates()} is driven by the cancellation flag alone and
   * does not require {@code timesModified} to be {@code true}.
   */
  @Test
  void journeyLevelCancellation_withNoTimes_isCanceled_butTimesNotModified() {
    var env = ENV_BUILDER.addTrip(TWO_STOP_TRIP).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);

    // "C U": cancelled (C) and has any updates (U) due to the cancellation flag alone;
    // scheduled times are preserved unchanged because no time modifications were applied.
    assertEquals(
      "C U | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }
}
