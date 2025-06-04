package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * Delays should be applied to the first trip but should leave the second trip untouched.
 */
class DelayedTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);

  private static final int DELAY = 1;
  private static final int STOP_SEQUENCE = 1;
  private static final FeedScopedId TRIP_ID = id(TRIP_1_ID);

  @Test
  void singleStopDelay() {
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "0:00:10", "0:00:11")
      .addStop(STOP_B, "0:00:20", "0:00:21")
      .build();
    var env = ENV_BUILDER.addTrip(tripInput).build();

    var tripUpdate = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(STOP_SEQUENCE, DELAY)
      .build();

    var result = env.applyTripUpdate(tripUpdate);

    assertEquals(1, result.successful());

    var pattern1 = env.getPatternForTrip(TRIP_1_ID);

    var snapshot = env.getTimetableSnapshot();
    var trip1Realtime = snapshot.resolve(pattern1, SERVICE_DATE);
    var trip1Scheduled = snapshot.resolve(pattern1, null);

    assertNotSame(trip1Realtime, trip1Scheduled);
    assertNotSame(trip1Realtime.getTripTimes(TRIP_ID), trip1Scheduled.getTripTimes(TRIP_ID));
    assertEquals(DELAY, trip1Realtime.getTripTimes(TRIP_ID).getArrivalDelay(STOP_SEQUENCE));
    assertEquals(DELAY, trip1Realtime.getTripTimes(TRIP_ID).getDepartureDelay(STOP_SEQUENCE));

    assertEquals(RealTimeState.SCHEDULED, trip1Scheduled.getTripTimes(TRIP_ID).getRealTimeState());

    assertEquals(
      "SCHEDULED | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.getScheduledTimetable(TRIP_1_ID)
    );
    assertEquals(
      "UPDATED | A [ND] 0:00:10 0:00:11 | B 0:00:21 0:00:22",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  /**
   * Tests delays to multiple stop times, where arrival and departure do not have the same delay.
   */
  @Test
  void complexDelay() {
    var tripInput = TripInput.of(TRIP_2_ID)
      .addStop(STOP_A, "0:01:00", "0:01:01")
      .addStop(STOP_B, "0:01:10", "0:01:11")
      .addStop(STOP_C, "0:01:20", "0:01:21")
      .build();
    var env = ENV_BUILDER.addTrip(tripInput).build();

    var tripUpdate = new TripUpdateBuilder(TRIP_2_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(0, 0)
      .addDelayedStopTime(1, 60, 80)
      .addDelayedStopTime(2, 90, 90)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));

    var snapshot = env.getTimetableSnapshot();

    var trip2 = env.getTransitService().getTrip(id(TRIP_2_ID));
    var originalTripPattern = env.getTransitService().findPattern(trip2);

    var originalTimetableForToday = snapshot.resolve(originalTripPattern, SERVICE_DATE);
    var originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

    assertNotSame(originalTimetableForToday, originalTimetableScheduled);

    var tripTimes = originalTimetableScheduled.getTripTimes(id(TRIP_2_ID));
    assertNotNull(tripTimes, "Original trip should be found in scheduled time table");
    assertFalse(
      tripTimes.isCanceledOrDeleted(),
      "Original trip times should not be canceled in scheduled time table"
    );
    assertEquals(RealTimeState.SCHEDULED, tripTimes.getRealTimeState());

    var realtimeTt = originalTimetableForToday.getTripTimes(id(TRIP_2_ID));
    assertNotNull(realtimeTt, "Original trip should be found in time table for service date");

    assertEquals(
      "SCHEDULED | A 0:01 0:01:01 | B 0:01:10 0:01:11 | C 0:01:20 0:01:21",
      env.getScheduledTimetable(TRIP_2_ID)
    );
    assertEquals(
      "UPDATED | A 0:01 0:01:01 | B 0:02:10 0:02:31 | C 0:02:50 0:02:51",
      env.getRealtimeTimetable(TRIP_2_ID)
    );
  }

  @Test
  void delayedAfterNextStopDeparture() {
    var tripInput = TripInput.of(TRIP_2_ID)
      .addStop(STOP_A, "0:00:00", "0:00:00")
      // 5-minute dwell
      .addStop(STOP_B, "0:05:00", "0:10:00")
      .addStop(STOP_C, "0:15:00", "0:16:00")
      .addStop(STOP_D, "0:20:00", "0:20:00")
      .build();
    var env = RealtimeTestEnvironment.of().addTrip(tripInput).build();

    var tripUpdate = new TripUpdateBuilder(TRIP_2_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(0, 0)
      .addDelayedArrivalStopTime(1, 900) // 00:20 arr
      .addDelayedStopTime(2, 540) // 00:24 arr / 00:25 dep
      .addDelayedDepartureStopTime(3, 420) // 00:27 dep
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));

    var snapshot = env.getTimetableSnapshot();

    var trip2 = env.getTransitService().getTrip(id(TRIP_2_ID));
    var originalTripPattern = env.getTransitService().findPattern(trip2);

    var originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

    var originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(id(TRIP_2_ID));
    assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());

    assertEquals(
      "SCHEDULED | A 0:00 0:00 | B 0:05 0:10 | C 0:15 0:16 | D 0:20 0:20",
      env.getScheduledTimetable(TRIP_2_ID)
    );
    assertEquals(
      "UPDATED | A 0:00 0:00 | B 0:20 0:20 | C 0:24 0:25 | D 0:27 0:27",
      env.getRealtimeTimetable(TRIP_2_ID)
    );
  }
}
