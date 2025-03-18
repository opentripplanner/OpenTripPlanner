package org.opentripplanner.updater.trip.siri.moduletests.fuzzymatching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class FuzzyTripMatchingTest implements RealtimeTestConstants {

  private static final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .build();

  /**
   * Update calls without changing the pattern. Fuzzy matching.
   */
  @Test
  void testUpdateJourneyWithFuzzyMatching() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = updatedJourneyBuilder(env).buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetableWithFuzzyMatcher(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
  }

  /**
   * Update calls without changing the pattern. Fuzzy matching.
   * Edge case: invalid reference to vehicle journey and missing aimed departure time.
   */
  @Test
  void testUpdateJourneyWithFuzzyMatchingAndMissingAimedDepartureTime() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(SERVICE_DATE).withVehicleJourneyRef("XXX")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected(null, "00:00:12")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:20", "00:00:22")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetableWithFuzzyMatcher(updates);
    assertEquals(0, result.successful(), "Should fail gracefully");
    assertFailure(UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH, result);
  }

  private static SiriEtBuilder updatedJourneyBuilder(RealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper()).withEstimatedCalls(builder ->
      builder
        .call(STOP_A1)
        .departAimedExpected("00:00:11", "00:00:15")
        .call(STOP_B1)
        .arriveAimedExpected("00:00:20", "00:00:25")
    );
  }

  private static void assertTripUpdated(RealtimeTestEnvironment env) {
    assertEquals(
      "UPDATED | A1 0:00:15 0:00:15 | B1 0:00:25 0:00:25",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }
}
