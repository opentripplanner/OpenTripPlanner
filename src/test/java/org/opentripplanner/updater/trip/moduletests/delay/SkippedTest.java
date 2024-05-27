package org.opentripplanner.updater.trip.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.trip.GtfsRealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * A mixture of delayed and skipped stops should result in both delayed and cancelled stops.
 */
public class SkippedTest {

  @Test
  public void scheduledTripWithSkippedAndScheduled() {
    var env = new GtfsRealtimeTestEnvironment();
    String scheduledTripId = env.trip2.getId().getId();

    var tripUpdate = new TripUpdateBuilder(
      scheduledTripId,
      env.serviceDate,
      SCHEDULED,
      env.timeZone
    )
      .addDelayedStopTime(0, 0)
      .addSkippedStop(1)
      .addDelayedStopTime(2, 90)
      .build();

    var result = env.applyTripUpdates(tripUpdate);

    assertEquals(1, result.successful());

    // THEN
    final TimetableSnapshot snapshot = env.source.getTimetableSnapshot();

    // Original trip pattern
    {
      final FeedScopedId tripId = env.trip2.getId();
      final Trip trip = env.transitModel.getTransitModelIndex().getTripForId().get(tripId);
      final TripPattern originalTripPattern = env.transitModel
        .getTransitModelIndex()
        .getPatternForTrip()
        .get(trip);

      final Timetable originalTimetableForToday = snapshot.resolve(
        originalTripPattern,
        env.serviceDate
      );
      final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

      assertNotSame(originalTimetableForToday, originalTimetableScheduled);

      final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(tripId);
      final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(
        originalTripIndexForToday
      );
      assertTrue(
        originalTripTimesForToday.isDeleted(),
        "Original trip times should be deleted in time table for service date"
      );
      // original trip should be canceled
      assertEquals(RealTimeState.DELETED, originalTripTimesForToday.getRealTimeState());
    }

    // New trip pattern
    {
      final TripPattern newTripPattern = snapshot.getRealtimeAddedTripPattern(
        env.trip2.getId(),
        env.serviceDate
      );

      final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, env.serviceDate);
      final Timetable newTimetableScheduled = snapshot.resolve(newTripPattern, null);

      assertNotSame(newTimetableForToday, newTimetableScheduled);

      assertTrue(newTripPattern.canBoard(0));
      assertFalse(newTripPattern.canBoard(1));
      assertTrue(newTripPattern.canBoard(2));

      final int newTimetableForTodayModifiedTripIndex = newTimetableForToday.getTripIndex(
        scheduledTripId
      );

      var newTripTimes = newTimetableForToday.getTripTimes(newTimetableForTodayModifiedTripIndex);
      assertEquals(RealTimeState.UPDATED, newTripTimes.getRealTimeState());

      assertEquals(
        -1,
        newTimetableScheduled.getTripIndex(scheduledTripId),
        "New trip should not be found in scheduled time table"
      );

      assertEquals(0, newTripTimes.getArrivalDelay(0));
      assertEquals(0, newTripTimes.getDepartureDelay(0));
      assertEquals(42, newTripTimes.getArrivalDelay(1));
      assertEquals(47, newTripTimes.getDepartureDelay(1));
      assertEquals(90, newTripTimes.getArrivalDelay(2));
      assertEquals(90, newTripTimes.getDepartureDelay(2));
      assertFalse(newTripTimes.isCancelledStop(0));
      assertTrue(newTripTimes.isCancelledStop(1));
      assertFalse(newTripTimes.isNoDataStop(2));
    }
  }
}
