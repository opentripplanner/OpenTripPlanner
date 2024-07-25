package org.opentripplanner.updater.trip.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.RealtimeTestEnvironment.SERVICE_DATE;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.TripTimesStringBuilder;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * A mixture of delayed and skipped stops should result in both delayed and cancelled stops.
 */
public class SkippedTest {

  @Test
  void scheduledTripWithSkippedAndScheduled() {
    var env = RealtimeTestEnvironment.gtfs();
    String scheduledTripId = env.trip2.getId().getId();

    var tripUpdate = new TripUpdateBuilder(scheduledTripId, SERVICE_DATE, SCHEDULED, env.timeZone)
      .addDelayedStopTime(0, 0)
      .addSkippedStop(1)
      .addDelayedStopTime(2, 90)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));

    assertOriginalTripPatternIsDeleted(env, env.trip2.getId());

    assertNewTripTimesIsUpdated(env, env.trip2.getId());

    assertEquals(
      "UPDATED | A1 0:01 0:01:01 | B1 [C] 0:01:52 0:01:58 | C1 0:02:50 0:02:51",
      env.getRealtimeTimetable(scheduledTripId)
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
    var env = RealtimeTestEnvironment.gtfs();
    var tripId = env.trip2.getId();

    var tripUpdate = new TripUpdateBuilder(tripId.getId(), SERVICE_DATE, SCHEDULED, env.timeZone)
      .addDelayedStopTime(0, 0)
      .addSkippedStop(1)
      .addDelayedStopTime(2, 90)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate, DIFFERENTIAL));

    // Create update to the same trip but now the skipped stop is no longer skipped
    var scheduledBuilder = new TripUpdateBuilder(
      tripId.getId(),
      SERVICE_DATE,
      SCHEDULED,
      env.timeZone
    )
      .addDelayedStopTime(0, 0)
      .addDelayedStopTime(1, 50)
      .addDelayedStopTime(2, 90);

    tripUpdate = scheduledBuilder.build();

    // apply the update with the previously skipped stop now scheduled
    assertSuccess(env.applyTripUpdate(tripUpdate, DIFFERENTIAL));

    // Check that the there is no longer a realtime added trip pattern for the trip and that the
    // stoptime updates have gone through
    var snapshot = env.getTimetableSnapshot();

    assertNull(snapshot.getRealtimeAddedTripPattern(tripId, SERVICE_DATE));

    assertNewTripTimesIsUpdated(env, tripId);

    assertEquals(
      "SCHEDULED | A1 0:01 0:01:01 | B1 0:01:10 0:01:11 | C1 0:01:20 0:01:21",
      env.getScheduledTimetable(tripId)
    );
    assertEquals(
      "UPDATED | A1 0:01 0:01:01 | B1 0:02 0:02:01 | C1 0:02:50 0:02:51",
      env.getRealtimeTimetable(tripId, SERVICE_DATE)
    );
  }

  /**
   * Tests a mixture of SKIPPED and NO_DATA.
   */
  @Test
  void skippedNoData() {
    var env = RealtimeTestEnvironment.gtfs();

    final FeedScopedId tripId = env.trip2.getId();

    var tripUpdate = new TripUpdateBuilder(tripId.getId(), SERVICE_DATE, SCHEDULED, env.timeZone)
      .addNoDataStop(0)
      .addSkippedStop(1)
      .addNoDataStop(2)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));

    assertOriginalTripPatternIsDeleted(env, tripId);

    assertNewTripTimesIsUpdated(env, tripId);

    assertEquals(
      "UPDATED | A1 [ND] 0:01 0:01:01 | B1 [C] 0:01:10 0:01:11 | C1 [ND] 0:01:20 0:01:21",
      env.getRealtimeTimetable(env.trip2)
    );
  }

  private static void assertOriginalTripPatternIsDeleted(
    RealtimeTestEnvironment env,
    FeedScopedId tripId
  ) {
    var trip = env.transitModel.getTransitModelIndex().getTripForId().get(tripId);
    var originalTripPattern = env.transitModel.getTransitModelIndex().getPatternForTrip().get(trip);
    var snapshot = env.getTimetableSnapshot();
    var originalTimetableForToday = snapshot.resolve(originalTripPattern, SERVICE_DATE);
    var originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

    assertNotSame(originalTimetableForToday, originalTimetableScheduled);

    int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(tripId);
    assertTrue(
      originalTripIndexScheduled > -1,
      "Original trip should be found in scheduled time table"
    );
    var originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(
      originalTripIndexScheduled
    );
    assertFalse(
      originalTripTimesScheduled.isCanceledOrDeleted(),
      "Original trip times should not be canceled in scheduled time table"
    );

    assertEquals(
      "SCHEDULED | A1 0:01 0:01:01 | B1 0:01:10 0:01:11 | C1 0:01:20 0:01:21",
      TripTimesStringBuilder.encodeTripTimes(originalTripTimesScheduled, originalTripPattern)
    );

    int originalTripIndexForToday = originalTimetableForToday.getTripIndex(tripId);
    assertTrue(
      originalTripIndexForToday > -1,
      "Original trip should be found in time table for service date"
    );
    var originalTripTimesForToday = originalTimetableForToday.getTripTimes(
      originalTripIndexForToday
    );
    assertTrue(
      originalTripTimesForToday.isDeleted(),
      "Original trip times should be deleted in time table for service date"
    );
    // original trip should be deleted
    assertEquals(RealTimeState.DELETED, originalTripTimesForToday.getRealTimeState());
  }

  private static void assertNewTripTimesIsUpdated(
    RealtimeTestEnvironment env,
    FeedScopedId tripId
  ) {
    var originalTripPattern = env.transitModel
      .getTransitModelIndex()
      .getPatternForTrip()
      .get(env.trip2);
    var snapshot = env.getTimetableSnapshot();
    var originalTimetableForToday = snapshot.resolve(originalTripPattern, SERVICE_DATE);

    var originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

    assertNotSame(originalTimetableForToday, originalTimetableScheduled);

    int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(tripId);

    assertTrue(
      originalTripIndexScheduled > -1,
      "Original trip should be found in scheduled time table"
    );
    var originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(
      originalTripIndexScheduled
    );
    assertFalse(
      originalTripTimesScheduled.isCanceledOrDeleted(),
      "Original trip times should not be canceled in scheduled time table"
    );
    assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());
    int originalTripIndexForToday = originalTimetableForToday.getTripIndex(tripId);

    assertTrue(
      originalTripIndexForToday > -1,
      "Original trip should be found in time table for service date"
    );
  }
}
