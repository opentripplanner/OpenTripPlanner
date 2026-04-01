package org.opentripplanner.updater.trip.gtfs.moduletests.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class NoArrivalTimeTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop(STOP_A_ID);
  private final RegularStop stopB = envBuilder.stop(STOP_B_ID);
  private final RegularStop stopC = envBuilder.stop(STOP_C_ID);
  private final TripInput tripInput = TripInput.of(TRIP_1_ID)
    .addStop(stopA, "10:00", "10:00")
    .addStop(stopB, "10:10", "10:10")
    .addStop(stopC, "10:20", "10:20");

  @Test
  void interpolateArrivalOnFirstStop() {
    var env = envBuilder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addStopTimeWithArrivalAndDeparture(0, null, "10:00")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));
  }

  @Test
  void interpolateArrivalOnMiddleStop() {
    var env = envBuilder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addStopTimeWithArrivalAndDeparture(0, "10:00", "10:00")
      .addStopTimeWithArrivalAndDeparture(1, null, "10:09")
      .addStopTimeWithArrivalAndDeparture(2, "10:20", "10:20")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));
    assertEquals(
      "UPDATED | A 10:00 10:00 | B 10:09 10:09 | C 10:20 10:20",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }
}
