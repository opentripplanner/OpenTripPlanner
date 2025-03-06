package org.opentripplanner.updater.trip.siri.moduletests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class InterpolationTest implements RealtimeTestConstants {

  private static final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .addStop(STOP_C1, "0:00:40", "0:00:41")
    .build();

  /**
   * This test shows that we accept a Siri message containing incomplete data.
   * The ET message contains only departure times for one of three calls. The current logic
   * interpolates the times for the omitted calls.
   *
   * This message is not valid according to the Siri nordic profile and we would probably want to
   * reject it instead.
   */
  @Test
  void testInterpolation() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder.call(STOP_B1).departAimedExpected("00:00:21", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    // The times for A1 are unchanged
    // The times for C1 are interpolated from the delay at B1
    assertEquals(
      "UPDATED | A1 0:00:10 0:00:11 | B1 0:00:20 0:00:25 | C1 0:00:44 0:00:45",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  /**
   * This test illustrates that we use the previous realtime information for missing calls.
   *
   * This message is not valid according to the Siri nordic profile and we would probably want to
   * reject it instead.
   */
  @Test
  void testInterpolationWithPreviousRealtime() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:20", "00:00:25")
          .departAimedExpected("00:00:21", "00:00:26")
          .call(STOP_C1)
          .arriveAimedExpected("00:00:40", "00:00:45")
          .departAimedExpected("00:00:41", "00:00:46")
      )
      .buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(1, result.successful());

    var updates2 = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder.call(STOP_B1).departAimedExpected("00:00:21", "00:00:31")
      )
      .buildEstimatedTimetableDeliveries();
    var result2 = env.applyEstimatedTimetable(updates2);
    assertEquals(1, result2.successful());

    // We use the previous realtime times for A1
    // We interpolate the times for C1
    assertEquals(
      "UPDATED | A1 0:00:15 0:00:15 | B1 0:00:20 0:00:31 | C1 0:00:50 0:00:51",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }
}
