package org.opentripplanner.updater.trip.siri.moduletests.fuzzymatching;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

class FuzzyTripMatchingTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  /**
   * Update calls without changing the pattern. Fuzzy matching.
   */
  @Test
  void testUpdateJourneyWithFuzzyMatching() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();
    var result = siri.applyEstimatedTimetableWithFuzzyMatcher(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
  }

  /**
   * Update calls without changing the pattern. Fuzzy matching.
   * Edge case: invalid reference to vehicle journey and missing aimed departure time.
   */
  @Test
  void testUpdateJourneyWithFuzzyMatchingAndMissingAimedDepartureTime() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(env.defaultServiceDate()).withVehicleJourneyRef("XXX")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected(null, "00:00:12")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:22")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetableWithFuzzyMatcher(updates);
    assertEquals(0, result.successful(), "Should fail gracefully");
    assertFailure(UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH, result);
  }

  private static void assertTripUpdated(TransitTestEnvironment env) {
    assertEquals(
      "UPDATED | A 0:00:15 0:00:15 | B 0:00:25 0:00:25",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }
}
