package org.opentripplanner.updater.trip.siri.moduletests.rejection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class IncompleteMessageTest implements RealtimeTestConstants {

  private static final TripInput TRIP_INPUT = TripInput
    .of(TRIP_1_ID)
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .addStop(STOP_C1, "0:00:40", "0:00:41")
    .build();

  /**
   * This test shows that we reject a Siri message containing incomplete data.
   */
  @Test
  void testInCompleteMessage() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder.call(STOP_B1).departAimedExpected("00:00:21", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.failed());
    assertEquals(Set.of(TOO_FEW_STOPS), result.failures().keySet());
  }
}
