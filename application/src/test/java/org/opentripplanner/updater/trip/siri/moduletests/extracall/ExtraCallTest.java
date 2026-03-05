package org.opentripplanner.updater.trip.siri.moduletests.extracall;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;

class ExtraCallTest implements RealtimeTestConstants {

  private final String ROUTE_ID = "route-id";

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stopAtStation(STOP_A_ID, "A");
  private final RegularStop STOP_B = ENV_BUILDER.stopAtStation(STOP_B_ID, "B");
  private final RegularStop STOP_C = ENV_BUILDER.stopAtStation(STOP_C_ID, "C");
  private final RegularStop STOP_D = ENV_BUILDER.stopAtStation(STOP_D_ID, "D");
  private final Route ROUTE = ENV_BUILDER.route(ROUTE_ID);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .withRoute(ROUTE)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  @Test
  void testExtraCall() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updateWithExtraCall(siri);

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | D [EC] 0:00:20 0:00:25 | B 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * Apply the same extra call update twice (identical message). Verifies idempotency: the trip
   * times and the MODIFIED pattern are unchanged after the second application.
   */
  @Test
  void testExtraCallMultipleTimes() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updateWithExtraCall(siri);
    siri.applyEstimatedTimetable(updates);
    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | D [EC] 0:00:20 0:00:25 | B 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  @Test
  void testExtraCallAndCancellation() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updateWithExtraCall(siri);
    siri.applyEstimatedTimetable(updates);
    var result = siri.applyEstimatedTimetable(updates);

    var cancellation = new SiriEtBuilder(env.localTimeParser())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var cancellationResult = siri.applyEstimatedTimetable(cancellation);

    assertSuccess(cancellationResult);

    assertSuccess(result);
    assertEquals(
      "CANCELED | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * Add an extra call (A → D(extra) → B), then send a second update with the same extra call
   * but different times. Unlike {@link #testExtraCallMultipleTimes()} which replays an identical
   * message, this test verifies that updated times are actually applied while preserving the
   * extra call and the MODIFIED pattern.
   */
  @Test
  void testExtraCallThenUpdateTimesKeepsExtraCall() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // Step 1: Add extra call D between A and B
    var extraCallUpdate = updateWithExtraCall(siri);
    assertSuccess(siri.applyEstimatedTimetable(extraCallUpdate));

    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | D [EC] 0:00:20 0:00:25 | B 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:route-id::001:RT[MODIFIED]"
    );

    // Step 2: Send update with same extra call but different times
    var updatedTimes = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withLineRef(ROUTE_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:16"))
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_D)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:18", "00:00:22")
          .departAimedExpected("00:00:19", "00:00:27")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:35")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updatedTimes);
    assertSuccess(result);

    // Extra call D should still be present with updated times
    assertEquals(
      "MODIFIED | A [R] 0:00:16 0:00:16 | D [EC] 0:00:22 0:00:27 | B 0:00:35 0:00:35",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    var patterns = env.raptorData().summarizePatterns();
    assertThat(patterns).hasSize(1);
    assertThat(patterns.stream().findFirst().get()).endsWith("[MODIFIED]");
  }

  /**
   * Add an extra call (A → D(extra) → B), then send a regular update without the extra call
   * (A → B with updated times). The trip should revert to the scheduled pattern.
   */
  @Test
  void testExtraCallThenRevertToOriginalStops() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // Step 1: Add extra call D between A and B
    var extraCallUpdate = updateWithExtraCall(siri);
    assertSuccess(siri.applyEstimatedTimetable(extraCallUpdate));

    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | D [EC] 0:00:20 0:00:25 | B 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:route-id::001:RT[MODIFIED]"
    );

    // Step 2: Send regular update without extra call — just A → B with updated times
    var revert = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:16"))
      .withEstimatedCalls(builder ->
        builder.call(STOP_B).arriveAimedExpected("00:00:20", "00:00:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(revert);
    assertSuccess(result);

    // Trip should revert to the scheduled pattern with UPDATED state
    assertEquals(
      "UPDATED | A [R] 0:00:16 0:00:16 | B 0:00:30 0:00:30",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[UPDATED]");
  }

  @Test
  void testExtraUnknownStop() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected extra stop without isExtraCall flag
          .call(STOP_D)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(STOP_B)
          .arriveAimedExpected("00:00:30", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.TOO_MANY_STOPS, result);
  }

  @Test
  void testExtraCallSameNumberOfStops() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected ExtraCall flag on a scheduled stop
          .call(STOP_B)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:30", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE, result);
  }

  @Test
  void testExtraCallAndIllegalChangeOfOtherStops() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_D)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          // this scheduled stop should not be changed
          .call(STOP_C)
          .arriveAimedExpected("00:00:30", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.STOP_MISMATCH, result);
  }

  private List<EstimatedTimetableDeliveryStructure> updateWithExtraCall(SiriTestHelper siri) {
    return siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withLineRef(ROUTE_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_D)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:18", "00:00:20")
          .departAimedExpected("00:00:19", "00:00:25")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();
  }
}
