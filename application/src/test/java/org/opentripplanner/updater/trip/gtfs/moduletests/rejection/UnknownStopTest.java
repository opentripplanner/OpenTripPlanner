package org.opentripplanner.updater.trip.gtfs.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.UNKNOWN_STOP;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class UnknownStopTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder builder = TransitTestEnvironment.of();
  private final RegularStop stopA = builder.stop(STOP_A_ID);
  private final RegularStop stopB = builder.stop(STOP_B_ID);

  @Test
  void unknownStop() {
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(stopA, "10:00", "10:00")
      .addStop(stopB, "10:10", "10:20");
    var env = builder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);
    var update = rt.tripUpdateScheduled(TRIP_1_ID).addStopTime("unknown stop", "10:00").build();

    assertFailure(UNKNOWN_STOP, rt.applyTripUpdate(update));
  }
}
