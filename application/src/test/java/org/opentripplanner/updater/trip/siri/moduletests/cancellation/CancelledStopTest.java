package org.opentripplanner.updater.trip.siri.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class CancelledStopTest implements RealtimeTestConstants {

  private static final TripInput TRIP_INPUT = TripInput.of(TRIP_2_ID)
    .addStop(STOP_A1, "0:01:00", "0:01:01")
    .addStop(STOP_B1, "0:01:10", "0:01:11")
    .addStop(STOP_C1, "0:01:20", "0:01:21")
    .build();

  @Test
  void testCancelStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_2_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:01:01", "00:01:01")
          .call(STOP_B1)
          .withIsCancellation(true)
          .call(STOP_C1)
          .arriveAimedExpected("00:01:30", "00:01:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 0:01:01 0:01:01 | B1 [C] 0:01:10 0:01:11 | C1 0:01:30 0:01:30",
      env.getRealtimeTimetable(TRIP_2_ID)
    );
  }
}
