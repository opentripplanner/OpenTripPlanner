package org.opentripplanner.updater.trip.gtfs.moduletests.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class BackwardsEarlinessPropagationCancellationTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop(STOP_A_ID);
  private final RegularStop stopB = envBuilder.stop(STOP_B_ID);
  private final RegularStop stopC = envBuilder.stop(STOP_C_ID);
  private final RegularStop stopD = envBuilder.stop(STOP_D_ID);
  private final TripInput tripInput = TripInput.of(TRIP_1_ID)
    .addStop(stopA, "10:00", "10:00")
    .addStop(stopB, "10:10", "10:10")
    .addStop(stopC, "10:20", "10:20")
    .addStop(stopD, "10:30", "10:30");

  @Test
  void noDataAndCancellation() {
    var env = envBuilder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addNoDataStop(0)
      .addNoDataStop(1)
      .addSkippedStop(2)
      .addStopTime(3, "10:19")
      .build();

    assertSuccess(rt.applyTripUpdate(tripUpdate));
    assertEquals(
      "UPDATED | A [ND] 10:00 10:00 | B [ND] 10:10 10:10 | C [C] 10:19 10:19 | D 10:19 10:19",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  @Test
  @Disabled(
    "The real-time updater code doesn't currently process estimated times on a skipped stop"
  )
  void invalidEstimatedTimeOnSkippedStop() {
    var env = envBuilder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var tripUpdate = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addNoDataStop(0)
      .addNoDataStop(1)
      .addSkippedStop(2, "10:20")
      .addStopTime(3, "10:19")
      .build();

    assertFailure(NEGATIVE_HOP_TIME, rt.applyTripUpdate(tripUpdate));
  }
}
