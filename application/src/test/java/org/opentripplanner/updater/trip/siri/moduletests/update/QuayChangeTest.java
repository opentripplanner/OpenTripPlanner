package org.opentripplanner.updater.trip.siri.moduletests.update;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

class QuayChangeTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stopAtStation(STOP_B_ID, STATION_OMEGA_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stopAtStation(STOP_C_ID, STATION_OMEGA_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  /**
   * Change quay on a trip
   */
  @Test
  void testChangeQuay() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[SCHEDULED]");
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder.call(STOP_C).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | C 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );

    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Route1::001:RT[MODIFIED]");
  }

  /**
   * Change quay (B to C) producing a MODIFIED pattern, then send a second update referencing the
   * original stop B, reverting the pattern back to the scheduled one.
   */
  @Test
  void testChangeQuayThenRevertToOriginalStops() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // Step 1: Change quay from B to C (same station)
    var quayChange = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder.call(STOP_C).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result1 = siri.applyEstimatedTimetable(quayChange);
    assertSuccess(result1);
    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | C 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );

    // Step 2: Revert to original stop B with updated times
    var revert = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:16"))
      .withEstimatedCalls(builder ->
        builder.call(STOP_B).arriveAimedExpected("00:00:20", "00:00:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result2 = siri.applyEstimatedTimetable(revert);
    assertSuccess(result2);
    assertEquals(
      "UPDATED | A [R] 0:00:16 0:00:16 | B 0:00:30 0:00:30",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    // Pattern reverts to the scheduled one (not an RT pattern), but trip state is UPDATED
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[UPDATED]");
  }

  /**
   * Change quay (B to C), then send a second update that keeps the same quay change (C)
   * but with different delay. The quay change must be preserved across updates.
   */
  @Test
  void testChangeQuayThenUpdateTimesKeepsQuayChange() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // Step 1: Change quay from B to C (same station) with delay
    var quayChange = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder.call(STOP_C).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result1 = siri.applyEstimatedTimetable(quayChange);
    assertSuccess(result1);
    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | C 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Route1::001:RT[MODIFIED]");

    // Step 2: Keep quay change (still C) but with different delay
    var updatedTimes = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:16"))
      .withEstimatedCalls(builder ->
        builder.call(STOP_C).arriveAimedExpected("00:00:20", "00:00:35")
      )
      .buildEstimatedTimetableDeliveries();

    var result2 = siri.applyEstimatedTimetable(updatedTimes);
    assertSuccess(result2);
    // Quay change must be preserved - still MODIFIED on RT pattern, not reverted to scheduled
    assertEquals(
      "MODIFIED | A [R] 0:00:16 0:00:16 | C 0:00:35 0:00:35",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Route1::001:RT[MODIFIED]");
  }
}
