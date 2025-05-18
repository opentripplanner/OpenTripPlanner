package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.TripTimesStringBuilder;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * A mixture of delayed and skipped stops should result in both delayed and cancelled stops.
 */
class SkippedTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_2_ID)
    .addStop(STOP_A, "0:01:00", "0:01:01")
    .addStop(STOP_B, "0:01:10", "0:01:11")
    .addStop(STOP_C, "0:01:20", "0:01:21")
    .build();

  @Test
  void scheduledTripWithSkippedAndScheduled() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    var tripUpdate = new TripUpdateBuilder(TRIP_2_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(0, 0)
      .addSkippedStop(1)
      .addDelayedStopTime(2, 90)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));

    assertOriginalTripPatternIsDeleted(env, TRIP_2_ID);

    assertNewTripTimesIsUpdated(env, TRIP_2_ID);

    assertEquals(
      "UPDATED | A 0:01 0:01:01 | B [C] 0:01:52 0:01:58 | C 0:02:50 0:02:51",
      env.getRealtimeTimetable(TRIP_2_ID)
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

    var tripUpdate = new TripUpdateBuilder(TRIP_2_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(0, 0)
      .addSkippedStop(1)
      .addDelayedStopTime(2, 90)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate, DIFFERENTIAL));

    // Create update to the same trip but now the skipped stop is no longer skipped
    var scheduledBuilder = new TripUpdateBuilder(TRIP_2_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(0, 0)
      .addDelayedStopTime(1, 50)
      .addDelayedStopTime(2, 90);

    tripUpdate = scheduledBuilder.build();

    // apply the update with the previously skipped stop now scheduled
    assertSuccess(env.applyTripUpdate(tripUpdate, DIFFERENTIAL));

    // Check that the there is no longer a realtime added trip pattern for the trip and that the
    // stoptime updates have gone through
    var snapshot = env.getTimetableSnapshot();

    assertNull(snapshot.getNewTripPatternForModifiedTrip(id(TRIP_2_ID), SERVICE_DATE));

    assertNewTripTimesIsUpdated(env, TRIP_2_ID);

    assertEquals(
      "SCHEDULED | A 0:01 0:01:01 | B 0:01:10 0:01:11 | C 0:01:20 0:01:21",
      env.getScheduledTimetable(TRIP_2_ID)
    );
    assertEquals(
      "UPDATED | A 0:01 0:01:01 | B 0:02 0:02:01 | C 0:02:50 0:02:51",
      env.getRealtimeTimetable(id(TRIP_2_ID), SERVICE_DATE)
    );
  }

  /**
   * Tests a mixture of SKIPPED and NO_DATA.
   */
  @Test
  void skippedNoData() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    String tripId = TRIP_2_ID;

    var tripUpdate = new TripUpdateBuilder(tripId, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addNoDataStop(0)
      .addSkippedStop(1)
      .addNoDataStop(2)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));

    assertOriginalTripPatternIsDeleted(env, tripId);

    assertNewTripTimesIsUpdated(env, tripId);

    assertEquals(
      "UPDATED | A [ND] 0:01 0:01:01 | B [C] 0:01:10 0:01:11 | C [ND] 0:01:20 0:01:21",
      env.getRealtimeTimetable(tripId)
    );
  }

  private static void assertOriginalTripPatternIsDeleted(
    RealtimeTestEnvironment env,
    String tripId
  ) {
    var trip = env.getTransitService().getTrip(id(tripId));
    var originalTripPattern = env.getTransitService().findPattern(trip);
    var snapshot = env.getTimetableSnapshot();
    var originalTimetableForToday = snapshot.resolve(originalTripPattern, SERVICE_DATE);
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

  private static void assertNewTripTimesIsUpdated(RealtimeTestEnvironment env, String tripId) {
    var trip = env.getTransitService().getTrip(id(tripId));
    var originalTripPattern = env.getTransitService().findPattern(trip);
    var snapshot = env.getTimetableSnapshot();
    var originalTimetableForToday = snapshot.resolve(originalTripPattern, SERVICE_DATE);

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
