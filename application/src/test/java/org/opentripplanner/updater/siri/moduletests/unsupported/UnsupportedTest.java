package org.opentripplanner.updater.siri.moduletests.unsupported;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.siri.SiriEtBuilder;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;

public class UnsupportedTest implements RealtimeTestConstants {

  private static final TripInput TRIP_1_INPUT = TripInput
    .of(TRIP_1_ID)
    .withRoute(ROUTE_1.copy().withOperator(OPERATOR1).build())
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .build();

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testAddStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A1).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_D1)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 0:00:15 0:00:15 | D1 [C] 0:00:20 0:00:25 | B1 0:00:33 0:00:33",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  /////////////////
  // Error cases //
  /////////////////

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testExtraUnknownStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected extra stop without isExtraCall flag
          .call(STOP_D1)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE, result);
  }
}
