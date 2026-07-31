package org.opentripplanner.updater.trip.gtfs.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.gtfs.GtfsRtTestHelper;

/**
 * Start dates that don't adhere to the format will be rejected.
 */
class MalformedStartDateTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop(STOP_A_ID);
  private final RegularStop stopB = envBuilder.stop(STOP_B_ID);

  @Test
  void malformedStartDate() {
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(stopA, "10:00", "10:00")
      .addStop(stopB, "10:01", "10:01");

    var env = envBuilder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var update = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .withStartDate("January 24th 2026")
      .addDelayedStopTime(0, 60)
      .build();

    assertFailure(INVALID_INPUT_STRUCTURE, rt.applyTripUpdate(update));
  }
}
