package org.opentripplanner.updater.trip.gtfs.moduletests.delay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class CircularRouteStopIdTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder builder = TransitTestEnvironment.of();
  private final RegularStop stopA = builder.stop(STOP_A_ID);
  private final RegularStop stopB = builder.stop(STOP_B_ID);
  private final TripInput tripInput = TripInput.of(TRIP_1_ID)
    .addStop(stopA, "10:00", "10:00")
    .addStop(stopB, "10:10", "10:10")
    .addStop(stopA, "10:20", "10:20");

  /**
   * It's quite questionable whether this should really be supported.
   */
  @Test
  void onlyStopIds() {
    var env = builder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);
    var update = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addStopTime(STOP_A_ID, "10:01")
      .addStopTime(STOP_B_ID, "10:21")
      .addStopTime(STOP_A_ID, "10:31")
      .build();

    assertSuccess(rt.applyTripUpdate(update));

    assertEquals(
      "UPDATED | A 10:01 10:01 | B 10:21 10:21 | A 10:31 10:31",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }
}
