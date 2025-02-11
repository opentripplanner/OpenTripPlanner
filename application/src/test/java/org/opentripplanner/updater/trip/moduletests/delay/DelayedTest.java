package org.opentripplanner.updater.trip.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * Delays should be applied to the first trip but should leave the second trip untouched.
 */
class DelayedTest implements RealtimeTestConstants {

  private static final int DELAY = 1;
  private static final int STOP_SEQUENCE = 1;

  @Test
  void singleStopDelay() {
    var TRIP_INPUT = TripInput
      .of(TRIP_1_ID)
      .addStop(STOP_A1, "0:00:10", "0:00:11")
      .addStop(STOP_B1, "0:00:20", "0:00:21")
      .build();
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var tripUpdate = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(STOP_SEQUENCE, DELAY)
      .build();

    var result = env.applyTripUpdate(tripUpdate);

    assertEquals(1, result.successful());

    var pattern1 = env.getPatternForTrip(TRIP_1_ID);
    int trip1Index = pattern1.getScheduledTimetable().getTripIndex(id(TRIP_1_ID));

    var snapshot = env.getTimetableSnapshot();
    var trip1Realtime = snapshot.resolve(pattern1, SERVICE_DATE);
    var trip1Scheduled = snapshot.resolve(pattern1, null);

    assertNotSame(trip1Realtime, trip1Scheduled);
    assertNotSame(trip1Realtime.getTripTimes(trip1Index), trip1Scheduled.getTripTimes(trip1Index));
    assertEquals(DELAY, trip1Realtime.getTripTimes(trip1Index).getArrivalDelay(STOP_SEQUENCE));
    assertEquals(DELAY, trip1Realtime.getTripTimes(trip1Index).getDepartureDelay(STOP_SEQUENCE));

    assertEquals(
      RealTimeState.SCHEDULED,
      trip1Scheduled.getTripTimes(trip1Index).getRealTimeState()
    );

    assertEquals(
      "SCHEDULED | A1 0:00:10 0:00:11 | B1 0:00:20 0:00:21",
      env.getScheduledTimetable(TRIP_1_ID)
    );
    assertEquals(
      "UPDATED | A1 [ND] 0:00:10 0:00:11 | B1 0:00:21 0:00:22",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  /**
   * Tests delays to multiple stop times, where arrival and departure do not have the same delay.
   */
  @Test
  void complexDelay() {
    var tripInput = TripInput
      .of(TRIP_2_ID)
      .addStop(STOP_A1, "0:00:00", "0:00:00")
      // 5-minute dwell
      .addStop(STOP_B1, "0:05:00", "0:10:00")
      .addStop(STOP_C1, "0:15:00", "0:16:00")
      .addStop(STOP_D1, "0:20:00", "0:20:00")
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

    var originalTimetableForToday = snapshot.resolve(originalTripPattern, SERVICE_DATE);
    var originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

    assertNotSame(originalTimetableForToday, originalTimetableScheduled);

    final int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(TRIP_2_ID);
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

    final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(TRIP_2_ID);
    assertTrue(
      originalTripIndexForToday > -1,
      "Original trip should be found in time table for service date"
    );

    assertEquals(
      "SCHEDULED | A1 0:00 0:00 | B1 0:05 0:10 | C1 0:15 0:16 | D1 0:20 0:20",
      env.getScheduledTimetable(TRIP_2_ID)
    );
    assertEquals(
      "UPDATED | A1 0:00 0:00 | B1 0:20 0:20 | C1 0:24 0:25 | D1 0:27 0:27",
      env.getRealtimeTimetable(TRIP_2_ID)
    );
  }
}
