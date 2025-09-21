package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

/**
 * Tests that stops can be SKIPPED for a trip which repeats times for consecutive stops.
 *
 * @link <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/6848">issue</a>
 */
class SkippedWithRepeatedTimesTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "10:00:00", "10:01:00")
    .addStop(STOP_B, "10:01:00", "10:01:00")
    .addStop(STOP_C, "10:01:00", "10:02:00")
    .build();

  @Test
  void skippedWithRepeatedTimes() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    var tripUpdate = new TripUpdateBuilder(TRIP_1_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addStopTime(STOP_A_ID, "10:00:00")
      .addSkippedStop(STOP_B_ID, "10:01:00")
      .addStopTime(STOP_C_ID, "10:01:00")
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));

    assertEquals(
      "UPDATED | A 10:00 10:00 | B [C] 10:00 10:00 | C 10:01 10:01",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }
}
