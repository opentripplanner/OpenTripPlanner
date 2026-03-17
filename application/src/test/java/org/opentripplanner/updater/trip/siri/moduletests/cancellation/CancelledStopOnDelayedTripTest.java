package org.opentripplanner.updater.trip.siri.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

/**
 * Tests that a cancelled stop on a delayed trip gets correct real-time times applied.
 * <p>
 * When a stop is cancelled on a delayed trip, the real-time times from the SIRI message must still
 * be applied to avoid NEGATIVE_HOP_TIME errors. Otherwise, the cancelled stop retains
 * scheduled times while surrounding stops have delayed times, causing the previous stop's
 * departure to exceed the cancelled stop's arrival.
 */
class CancelledStopOnDelayedTripTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:02", "0:02")
    .addStop(STOP_B, "0:04", "0:04")
    .addStop(STOP_C, "0:06", "0:06")
    .addStop(STOP_D, "0:08", "0:08");

  @Test
  void cancelledStopOnDelayedTripShouldApplyRealtimeTimes() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // Trip is delayed ~5 minutes. Stop C is cancelled but has expected times in the message.
    // Without applying the RT times, C would retain scheduled arrival 0:06 while B departs at 0:09 -> NEGATIVE_HOP_TIME
    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .arriveAimedExpected("00:02:00", "00:02:00")
          .departAimedExpected("00:02:00", "00:07:00")
          .call(STOP_B)
          .arriveAimedExpected("00:04:00", "00:09:00")
          .departAimedExpected("00:04:00", "00:09:00")
          .call(STOP_C)
          .withIsCancellation(true)
          .arriveAimedExpected("00:06:00", "00:11:00")
          .departAimedExpected("00:06:00", "00:11:00")
          .call(STOP_D)
          .arriveAimedExpected("00:08:00", "00:13:00")
          .departAimedExpected("00:08:00", "00:13:00")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "MODIFIED | A 0:02 0:07 | B 0:09 0:09 | C [C] 0:11 0:11 | D 0:13 0:13",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }
}
