package org.opentripplanner.updater.trip.siri.moduletests.rejection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.STOP_MISMATCH;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_MANY_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.UNKNOWN_STOP;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class InvalidCallsTest implements RealtimeTestConstants {

  private static final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .addStop(STOP_C1, "0:00:40", "0:00:41")
    .build();

  @Test
  void testTooFewCalls() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:10", "00:00:11")
          .call(STOP_B1)
          .departAimedExpected("00:00:21", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.failed());
    assertEquals(Set.of(TOO_FEW_STOPS), result.failures().keySet());
  }

  @Test
  void testTooManyCalls() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:10", "00:00:11")
          .call(STOP_B1)
          .departAimedExpected("00:00:21", "00:00:25")
          .call(STOP_C1)
          .departAimedExpected("00:00:40", "00:00:41")
          .call(STOP_D1)
          .departAimedExpected("00:00:50", "00:00:51")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.failed());
    assertEquals(Set.of(TOO_MANY_STOPS), result.failures().keySet());
  }

  @Test
  void testMismatchedStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:10", "00:00:11")
          .call(STOP_D1)
          .departAimedExpected("00:00:21", "00:00:25")
          .call(STOP_C1)
          .departAimedExpected("00:00:40", "00:00:41")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.failed());
    assertEquals(Set.of(STOP_MISMATCH), result.failures().keySet());
  }

  @Test
  void testUnknownStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:10", "00:00:11")
          .call(TEST_MODEL.stop("Unknown stop").withParentStation(STATION_A).build())
          .departAimedExpected("00:00:21", "00:00:25")
          .call(STOP_C1)
          .departAimedExpected("00:00:40", "00:00:41")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.failed());
    assertEquals(Set.of(UNKNOWN_STOP), result.failures().keySet());
  }
}
