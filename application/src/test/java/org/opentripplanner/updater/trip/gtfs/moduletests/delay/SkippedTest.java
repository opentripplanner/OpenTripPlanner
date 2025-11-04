package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.TripTimesStringBuilder;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

/**
 * A mixture of delayed and skipped stops should result in both delayed and cancelled stops.
 */
class SkippedTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_2_ID)
    .addStop(STOP_A, "0:01:00", "0:01:01")
    .addStop(STOP_B, "0:01:10", "0:01:11")
    .addStop(STOP_C, "0:01:20", "0:01:21");

  @Test
  void scheduledTripWithSkippedAndScheduled() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_2_ID)
      .addDelayedStopTime(0, 0)
      .addSkippedStop(1)
      .addDelayedStopTime(2, 90)
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertOriginalTripPatternIsDeleted(env, TRIP_2_ID);

    assertNewTripTimesIsUpdated(env, TRIP_2_ID);

    assertEquals(
      "UPDATED | A 0:01 0:01:01 | B [C] 0:01:52 0:01:58 | C 0:02:50 0:02:51",
      env.tripData(TRIP_2_ID).showTimetable()
    );
  }

  /**
   * Test realtime system behavior under one very particular case from issue #5725.
   * When applying differential realtime updates, an update may cancel some stops on a trip. A
   * later update may then revert the trip back to its originally scheduled sequence of stops.
   * When this happens, we expect the trip to be associated with a new trip pattern (where some
   * stops have no pickup or dropoff) then dissociated from that new pattern and re-associated
   * with its originally scheduled pattern. Any trip times that were created in timetables under
   * the new stop-skipping trip pattern should also be removed.
   */
  @Test
  void scheduledTripWithPreviouslySkipped() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_2_ID)
      .addDelayedStopTime(0, 0)
      .addSkippedStop(1)
      .addDelayedStopTime(2, 90)
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate, DIFFERENTIAL));

    // Create update to the same trip but now the skipped stop is no longer skipped
    var scheduledBuilder = rt
      .tripUpdateScheduled(TRIP_2_ID)
      .addDelayedStopTime(0, 0)
      .addDelayedStopTime(1, 50)
      .addDelayedStopTime(2, 90);

    tripUpdate = scheduledBuilder.build();

    // apply the update with the previously skipped stop now scheduled
    assertSuccess(rt.applyTripUpdate(tripUpdate, DIFFERENTIAL));

    // Check that the there is no longer a realtime added trip pattern for the trip and that the
    // stoptime updates have gone through
    var snapshot = env.timetableSnapshot();

    assertNull(snapshot.getNewTripPatternForModifiedTrip(id(TRIP_2_ID), env.defaultServiceDate()));

    assertNewTripTimesIsUpdated(env, TRIP_2_ID);

    assertEquals(
      "SCHEDULED | A 0:01 0:01:01 | B 0:01:10 0:01:11 | C 0:01:20 0:01:21",
      env.tripData(TRIP_2_ID).showScheduledTimetable()
    );
    assertEquals(
      "UPDATED | A 0:01 0:01:01 | B 0:02 0:02:01 | C 0:02:50 0:02:51",
      env.tripData(TRIP_2_ID).showTimetable()
    );
  }

  /**
   * Tests a mixture of SKIPPED and NO_DATA.
   */
  @Test
  void skippedNoData() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    String tripId = TRIP_2_ID;

    var tripUpdate = rt
      .tripUpdateScheduled(tripId)
      .addNoDataStop(0)
      .addSkippedStop(1)
      .addNoDataStop(2)
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertOriginalTripPatternIsDeleted(env, tripId);

    assertNewTripTimesIsUpdated(env, tripId);

    assertEquals(
      "UPDATED | A [ND] 0:01 0:01:01 | B [C] 0:01:10 0:01:11 | C [ND] 0:01:20 0:01:21",
      env.tripData(tripId).showTimetable()
    );
  }

  private static void assertOriginalTripPatternIsDeleted(
    TransitTestEnvironment env,
    String tripId
  ) {
    var trip = env.transitService().getTrip(id(tripId));
    var originalTripPattern = env.transitService().findPattern(trip);
    var snapshot = env.timetableSnapshot();
    var originalTimetableForToday = snapshot.resolve(originalTripPattern, env.defaultServiceDate());
    var originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

    assertNotSame(originalTimetableForToday, originalTimetableScheduled);

    var tripTimes = originalTimetableScheduled.getTripTimes(id(tripId));
    assertNotNull(tripTimes, "Original trip should be found in scheduled time table");
    assertFalse(
      tripTimes.isCanceledOrDeleted(),
      "Original trip times should not be canceled in scheduled time table"
    );

    assertEquals(
      "SCHEDULED | A 0:01 0:01:01 | B 0:01:10 0:01:11 | C 0:01:20 0:01:21",
      TripTimesStringBuilder.encodeTripTimes(tripTimes, originalTripPattern)
    );

    var scheduledTripTimes = originalTimetableForToday.getTripTimes(id(tripId));
    assertNotNull(
      scheduledTripTimes,
      "Original trip should be found in time table for service date"
    );
    assertTrue(
      scheduledTripTimes.isDeleted(),
      "Original trip times should be deleted in time table for service date"
    );
    // original trip should be deleted
    assertEquals(RealTimeState.DELETED, scheduledTripTimes.getRealTimeState());
  }

  private static void assertNewTripTimesIsUpdated(TransitTestEnvironment env, String tripId) {
    var trip = env.transitService().getTrip(id(tripId));
    var originalTripPattern = env.transitService().findPattern(trip);
    var snapshot = env.timetableSnapshot();
    var originalTimetableForToday = snapshot.resolve(originalTripPattern, env.defaultServiceDate());

    var originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

    assertNotSame(originalTimetableForToday, originalTimetableScheduled);

    var tripTimes = originalTimetableScheduled.getTripTimes(id(tripId));

    assertNotNull(tripTimes, "Original trip should be found in scheduled time table");
    assertFalse(
      tripTimes.isCanceledOrDeleted(),
      "Original trip times should not be canceled in scheduled time table"
    );
    assertEquals(RealTimeState.SCHEDULED, tripTimes.getRealTimeState());
    var tt = originalTimetableForToday.getTripTimes(id(tripId));

    assertNotNull(tt, "Original trip should be found in time table for service date");
  }
}
