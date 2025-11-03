package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

/**
 * Delays should be applied to the first trip but should leave the second trip untouched.
 */
class DelayedTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private static final int DELAY = 1;
  private static final int STOP_SEQUENCE = 1;

  @Test
  void singleStopDelay() {
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(STOP_A, "0:00:10", "0:00:11")
      .addStop(STOP_B, "0:00:20", "0:00:21");
    var env = ENV_BUILDER.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addDelayedStopTime(STOP_SEQUENCE, DELAY)
      .build();

    var result = rt.applyTripUpdate(tripUpdate);

    assertEquals(1, result.successful());

    var tripData = env.tripData(TRIP_1_ID);

    var trip1Realtime = tripData.tripTimes();
    var trip1Scheduled = tripData.scheduledTripTimes();

    assertNotSame(trip1Realtime, trip1Scheduled);
    assertEquals(DELAY, trip1Realtime.getArrivalDelay(STOP_SEQUENCE));
    assertEquals(DELAY, trip1Realtime.getDepartureDelay(STOP_SEQUENCE));

    assertEquals(RealTimeState.SCHEDULED, trip1Scheduled.getRealTimeState());

    assertEquals(
      "SCHEDULED | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.tripData(TRIP_1_ID).showScheduledTimetable()
    );
    assertEquals(
      "UPDATED | A [ND] 0:00:10 0:00:11 | B 0:00:21 0:00:22",
      env.tripData(TRIP_1_ID).showTimetable()
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
      .addStop(STOP_C, "0:01:20", "0:01:21");
    var env = ENV_BUILDER.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_2_ID)
      .addDelayedStopTime(0, 0)
      .addDelayedStopTime(1, 60, 80)
      .addDelayedStopTime(2, 90, 90)
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    var tripData = env.tripData(TRIP_2_ID);
    var realtimeTripTimes = tripData.tripTimes();
    var scheduledTripTimes = tripData.scheduledTripTimes();

    assertNotSame(realtimeTripTimes, scheduledTripTimes);

    assertNotNull(scheduledTripTimes, "Original trip should be found in scheduled time table");
    assertFalse(
      scheduledTripTimes.isCanceledOrDeleted(),
      "Original trip times should not be canceled in scheduled time table"
    );
    assertEquals(RealTimeState.SCHEDULED, scheduledTripTimes.getRealTimeState());

    assertNotNull(
      realtimeTripTimes,
      "Original trip should be found in time table for service date"
    );

    assertEquals(
      "SCHEDULED | A 0:01 0:01:01 | B 0:01:10 0:01:11 | C 0:01:20 0:01:21",
      tripData.showScheduledTimetable()
    );
    assertEquals(
      "UPDATED | A 0:01 0:01:01 | B 0:02:10 0:02:31 | C 0:02:50 0:02:51",
      tripData.showTimetable()
    );
  }
}
