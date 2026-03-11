package org.opentripplanner.updater.trip.gtfs.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_STOP_REFERENCE;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class TooManyStopsTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder builder = TransitTestEnvironment.of();
  private final RegularStop stopA = builder.stop(STOP_A_ID);
  private final RegularStop stopB = builder.stop(STOP_B_ID);
  private final TripInput tripInput = TripInput.of(TRIP_1_ID)
    .addStop(stopA, "10:00", "10:00")
    .addStop(stopB, "10:10", "10:10");

  @Test
  void tooManyUpdates() {
    var env = builder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);
    var update = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addStopTime(STOP_A_ID, "10:01")
      .addStopTime(STOP_B_ID, "10:21")
      .addStopTime(STOP_C_ID, "10:31")
      .build();

    assertFailure(INVALID_STOP_REFERENCE, rt.applyTripUpdate(update));
  }
}
