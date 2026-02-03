package org.opentripplanner.updater.trip.gtfs.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

/**
 * Stop sequences must start at 0 or more and be increasing.
 */
class InvalidScheduledStopSequenceTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop(STOP_A_ID);
  private final RegularStop stopB = envBuilder.stop(STOP_B_ID);
  private final TripInput tripInput = TripInput.of(TRIP_1_ID)
    .addStop(stopA, "10:00", "10:00")
    .addStop(stopB, "10:01", "10:01");

  @Test
  void negativeStopSequence() {
    var env = envBuilder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var update = rt.tripUpdateScheduled(TRIP_1_ID).addDelayedStopTime(-1, 60).build();

    assertFailure(INVALID_STOP_SEQUENCE, rt.applyTripUpdate(update));
  }

  @Test
  void nonIncreasingStopSequence() {
    var env = envBuilder.addTrip(tripInput).build();
    var rt = GtfsRtTestHelper.of(env);

    var update = rt
      .tripUpdateScheduled(TRIP_1_ID)
      .addDelayedStopTime(2, 10)
      .addDelayedStopTime(1, 10)
      .build();

    assertFailure(INVALID_STOP_SEQUENCE, rt.applyTripUpdate(update));
  }
}
