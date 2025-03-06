package org.opentripplanner.updater.trip.siri.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class NegativeTimesTest implements RealtimeTestConstants {

  private static final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .build();

  private static final TripInput TRIP_2_INPUT = TripInput.of(TRIP_2_ID)
    .addStop(STOP_A1, "0:01:00", "0:01:01")
    .addStop(STOP_B1, "0:01:10", "0:01:11")
    .addStop(STOP_C1, "0:01:20", "0:01:21")
    .build();

  @Test
  void testNegativeHopTime() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedActual("00:00:11", "00:00:15")
          .call(STOP_B1)
          .arriveAimedActual("00:00:20", "00:00:14")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testNegativeDwellTime() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_2_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_2_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedActual("00:01:01", "00:01:01")
          .call(STOP_B1)
          .arriveAimedActual("00:01:10", "00:01:13")
          .departAimedActual("00:01:11", "00:01:12")
          .call(STOP_C1)
          .arriveAimedActual("00:01:20", "00:01:20")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME, result);
  }
}
