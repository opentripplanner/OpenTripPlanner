package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

class DestinationDisplayTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStopWithHeadsign(STOP_A, "0:10", "0:10", "Original A")
    .addStopWithHeadsign(STOP_B, "0:20", "0:20", "Original B")
    .addStopWithHeadsign(STOP_C, "0:30", "0:30", "Original C")
    .addStopWithHeadsign(STOP_D, "0:40", "0:40", "Original D");

  @Test
  void testUpdateJourneyWithDestinationDisplay() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);
    var update = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedActual("00:10", "00:10")
          .addDestinationDisplay("Updated A")
          .call(STOP_B)
          .departAimedActual("00:20", "00:20")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_C)
          .addDestinationDisplay("Updated C")
          .departAimedExpected("00:30", "00:30")
          .call(STOP_D)
          .arriveAimedExpected("00:40", "00:40")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(update);

    assertEquals(1, result.successful());

    var tt = env.tripData(TRIP_1_ID).tripTimes();
    assertEquals("Updated A", tt.getHeadsign(0).toString());
    assertEquals("Original B", tt.getHeadsign(1).toString());
    assertEquals("Updated C", tt.getHeadsign(2).toString());
    assertEquals("Original D", tt.getHeadsign(3).toString());
  }
}
