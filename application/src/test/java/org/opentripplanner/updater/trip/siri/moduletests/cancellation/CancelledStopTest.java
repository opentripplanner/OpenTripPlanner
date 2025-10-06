package org.opentripplanner.updater.trip.siri.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.SiteTestBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

class CancelledStopTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SiteTestBuilder.of().withStops(STOP_A_ID, STOP_B_ID, STOP_D_ID).build()
  );

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A_ID, "0:01:00", "0:01:01")
    .addStop(STOP_B_ID, "0:01:10", "0:01:11")
    .addStop(STOP_D_ID, "0:01:20", "0:01:21")
    .build();

  @Test
  void testCancelStop() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A_ID)
          .departAimedExpected("00:01:01", "00:01:01")
          .call(STOP_B_ID)
          .withIsCancellation(true)
          .call(STOP_D_ID)
          .arriveAimedExpected("00:01:30", "00:01:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "MODIFIED | A 0:01:01 0:01:01 | B [C] 0:01:10 0:01:11 | D 0:01:30 0:01:30",
      env.tripFetcher(TRIP_1_ID).showTimetable()
    );
  }
}
