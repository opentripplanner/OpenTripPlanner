package org.opentripplanner.updater.trip.siri.moduletests.extracall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.SiteTestBuilder;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.service.SiteRepository;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;

class ExtraCallTest implements RealtimeTestConstants {

  private final SiteRepository site = SiteTestBuilder.of()
    .withStopAtStation(STOP_A_ID, "A")
    .withStopAtStation(STOP_B_ID, "B")
    .withStopAtStation(STOP_C_ID, "C")
    .withStopAtStation(STOP_D_ID, "D")
    .build();

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(site);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A_ID, "0:00:10", "0:00:11")
    .addStop(STOP_B_ID, "0:00:20", "0:00:21")
    .build();

  @Test
  void testExtraCall() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updateWithExtraCall(siri);

    var result = siri.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | D 0:00:20 0:00:25 | B 0:00:33 0:00:33",
      env.tripFetcher(TRIP_1_ID).showTimetable()
    );
  }

  @Test
  void testExtraCallMultipleTimes() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updateWithExtraCall(siri);
    siri.applyEstimatedTimetable(updates);
    var result = siri.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A [R] 0:00:15 0:00:15 | D 0:00:20 0:00:25 | B 0:00:33 0:00:33",
      env.tripFetcher(TRIP_1_ID).showTimetable()
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

    assertEquals(1, cancellationResult.successful());

    assertEquals(1, result.successful());
    assertEquals(
      "CANCELED | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.tripFetcher(TRIP_1_ID).showTimetable()
    );
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
          .call(STOP_A_ID)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected extra stop without isExtraCall flag
          .call(STOP_D_ID)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(STOP_B_ID)
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
          .call(STOP_A_ID)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected ExtraCall flag on a scheduled stop
          .call(STOP_B_ID)
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
          .call(STOP_A_ID)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_D_ID)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          // this scheduled stop should not be changed
          .call(STOP_C_ID)
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
      .withLineRef(TRIP_1_INPUT.routeId())
      .withRecordedCalls(builder ->
        builder.call(STOP_A_ID).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_D_ID)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:18", "00:00:20")
          .departAimedExpected("00:00:19", "00:00:25")
          .call(STOP_B_ID)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();
  }
}
