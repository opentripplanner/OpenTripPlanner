package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.SiteTestBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

/**
 * Tests that stops can be SKIPPED for a trip which repeats times for consecutive stops.
 *
 * @link <a href="https://github.com/opentripplanner/OpenTripPlanner/issues/6848">issue</a>
 */
class SkippedWithRepeatedTimesTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SiteTestBuilder.of().withStops(STOP_A_ID, STOP_B_ID, STOP_C_ID).build()
  );

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A_ID, "10:00:00", "10:01:00")
    .addStop(STOP_B_ID, "10:01:00", "10:01:00")
    .addStop(STOP_C_ID, "10:01:00", "10:02:00")
    .build();

  @Test
  void skippedWithRepeatedTimes() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addStopTime(STOP_A_ID, "10:00:00")
      .addSkippedStop(STOP_B_ID, "10:01:00")
      .addStopTime(STOP_C_ID, "10:01:00")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));

    assertEquals(
      "UPDATED | A 10:00 10:00 | B [C] 10:00 10:00 | C 10:01 10:01",
      env.tripFetcher(TRIP_1_ID).showTimetable()
    );
  }
}
