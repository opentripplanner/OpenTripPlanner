package org.opentripplanner.updater.trip.gtfs.moduletests.cancellation;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

/**
 * Test canceling a trip after a pattern change (skipped stop). This exercises the revert path
 * where a modified pattern must be cleaned up before the cancellation is applied to the
 * scheduled pattern.
 */
class CancelAfterPatternChangeTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "0:01:00", "0:01:01")
    .addStop(STOP_B, "0:01:10", "0:01:11")
    .addStop(STOP_C, "0:01:20", "0:01:21");

  /**
   * First skip a stop (creating a modified pattern), then cancel the trip. The cancellation
   * should revert the pattern change and mark the trip as CANCELED on the scheduled pattern.
   */
  @Test
  void cancelScheduledTripAfterSkippedStop() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    // Step 1: Skip stop B — creates a modified pattern
    var skipUpdate = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addDelayedStopTime(0, 0)
      .addSkippedStop(1)
      .addDelayedStopTime(2, 90)
      .build();

    assertSuccess(rt.applyTripUpdate(skipUpdate, DIFFERENTIAL));

    // Verify the modified pattern was created
    var snapshot = env.timetableSnapshot();
    assertNotNull(
      snapshot.getNewTripPatternForModifiedTrip(id(TRIP_1_ID), env.defaultServiceDate()),
      "A modified trip pattern should exist after skipping a stop"
    );

    assertEquals(
      "UPDATED | A 0:01 0:01:01 | B [C] 0:01:52 0:01:58 | C 0:02:50 0:02:51",
      env.tripData(TRIP_1_ID).showTimetable()
    );

    // Step 2: Cancel the trip
    var cancelUpdate = rt.tripUpdate(TRIP_1_ID, CANCELED).build();
    assertSuccess(rt.applyTripUpdate(cancelUpdate, DIFFERENTIAL));

    // Verify the modified pattern is cleaned up
    snapshot = env.timetableSnapshot();
    assertNull(
      snapshot.getNewTripPatternForModifiedTrip(id(TRIP_1_ID), env.defaultServiceDate()),
      "Modified trip pattern should be removed after cancellation"
    );

    // Trip should be CANCELED on the scheduled pattern
    var tripData = env.tripData(TRIP_1_ID);
    assertEquals(RealTimeState.CANCELED, tripData.realTimeState());
    assertTrue(tripData.tripTimes().isCanceledOrDeleted());

    assertEquals(
      "CANCELED | A 0:01 0:01:01 | B 0:01:10 0:01:11 | C 0:01:20 0:01:21",
      tripData.showTimetable()
    );
  }
}
