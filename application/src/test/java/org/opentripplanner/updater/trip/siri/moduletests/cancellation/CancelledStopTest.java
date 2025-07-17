package org.opentripplanner.updater.trip.siri.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class CancelledStopTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "0:01:00", "0:01:01")
    .addStop(STOP_B, "0:01:10", "0:01:11")
    .addStop(STOP_D, "0:01:20", "0:01:21")
    .build();

  @Test
  void testCancelStop() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01:01", "00:01:01")
          .call(STOP_B)
          .withIsCancellation(true)
          .call(STOP_D)
          .arriveAimedExpected("00:01:30", "00:01:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "MODIFIED | A 0:01:01 0:01:01 | B [C] 0:01:10 0:01:11 | D 0:01:30 0:01:30",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }
}
