package org.opentripplanner.updater.trip.siri.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.SiteTestBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

class NegativeTimesTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SiteTestBuilder.of().withStops(STOP_A_ID, STOP_B_ID, STOP_C_ID).build()
  );

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A_ID, "0:00:10", "0:00:11")
    .addStop(STOP_B_ID, "0:00:20", "0:00:21")
    .build();

  private final TripInput TRIP_2_INPUT = TripInput.of(TRIP_2_ID)
    .addStop(STOP_A_ID, "0:01:00", "0:01:01")
    .addStop(STOP_B_ID, "0:01:10", "0:01:11")
    .addStop(STOP_C_ID, "0:01:20", "0:01:21")
    .build();

  @Test
  void testNegativeHopTime() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A_ID)
          .departAimedActual("00:00:11", "00:00:15")
          .call(STOP_B_ID)
          .arriveAimedActual("00:00:20", "00:00:14")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testNegativeDwellTime() {
    var env = ENV_BUILDER.addTrip(TRIP_2_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_2_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A_ID)
          .departAimedActual("00:01:01", "00:01:01")
          .call(STOP_B_ID)
          .arriveAimedActual("00:01:10", "00:01:13")
          .departAimedActual("00:01:11", "00:01:12")
          .call(STOP_C_ID)
          .arriveAimedActual("00:01:20", "00:01:20")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME, result);
  }
}
