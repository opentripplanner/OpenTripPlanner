package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class UpdatedTimesTest implements RealtimeTestConstants {

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21")
    .build();

  /**
   * Update calls without changing the pattern. Match trip by dated vehicle journey.
   */
  @Test
  void testUpdateJourneyWithDatedVehicleJourneyRef() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    var updates = updatedJourneyBuilder(env)
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
    assertEquals(
      "UPDATED | A 0:00:15 0:00:15 | B 0:00:25 0:00:25",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  /**
   * Update calls without changing the pattern. Match trip by framed vehicle journey.
   */
  @Test
  void testUpdateJourneyWithFramedVehicleJourneyRef() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    var updates = updatedJourneyBuilder(env)
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(SERVICE_DATE).withVehicleJourneyRef(TRIP_1_ID)
      )
      .buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
  }

  /**
   * Update calls without changing the pattern. Missing reference to vehicle journey.
   */
  @Test
  void testUpdateJourneyWithoutJourneyRef() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    var updates = updatedJourneyBuilder(env).buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.TRIP_NOT_FOUND, result);
  }

  private SiriEtBuilder updatedJourneyBuilder(RealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper()).withEstimatedCalls(builder ->
      builder
        .call(STOP_A)
        .departAimedExpected("00:00:11", "00:00:15")
        .call(STOP_B)
        .arriveAimedExpected("00:00:20", "00:00:25")
    );
  }

  private static void assertTripUpdated(RealtimeTestEnvironment env) {
    assertEquals(
      "UPDATED | A 0:00:15 0:00:15 | B 0:00:25 0:00:25",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }
}
