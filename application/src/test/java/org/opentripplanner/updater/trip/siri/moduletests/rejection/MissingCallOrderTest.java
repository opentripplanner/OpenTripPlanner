package org.opentripplanner.updater.trip.siri.moduletests.rejection;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.MISSING_CALL_ORDER;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.MIXED_CALL_ORDER_AND_VISIT_NUMBER;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;

class MissingCallOrderTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21")
    .addStop(STOP_C, "0:00:40", "0:00:41");

  @Test
  void rejectMissingOrderOnEstimatedCall() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .departAimedExpected("00:00:21", "00:00:25")
          .call(STOP_C)
          .arriveAimedExpected("00:00:40", "00:00:45")
          .clearOrder()
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertFailure(MISSING_CALL_ORDER, result);
  }

  @Test
  void rejectMissingOrderOnRecordedCall() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder ->
        builder.call(STOP_A).departAimedActual("00:00:11", "00:00:15").clearOrder()
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_B)
          .departAimedExpected("00:00:21", "00:00:25")
          .call(STOP_C)
          .arriveAimedExpected("00:00:40", "00:00:45")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertFailure(MISSING_CALL_ORDER, result);
  }

  @Test
  void acceptVisitNumberAsFallback() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A)
          .clearOrder()
          .withVisitNumber(1)
          .departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_B)
          .clearOrder()
          .withVisitNumber(2)
          .departAimedExpected("00:00:21", "00:00:25")
          .call(STOP_C)
          .clearOrder()
          .withVisitNumber(3)
          .arriveAimedExpected("00:00:40", "00:00:45")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);
  }

  @Test
  void rejectMixedOrderAndVisitNumber() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .withVisitNumber(1)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .departAimedExpected("00:00:21", "00:00:25")
          .call(STOP_C)
          .arriveAimedExpected("00:00:40", "00:00:45")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertFailure(MIXED_CALL_ORDER_AND_VISIT_NUMBER, result);
  }

  @Test
  void rejectInconsistentOrderAndVisitNumberAcrossCalls() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .departAimedExpected("00:00:21", "00:00:25")
          .call(STOP_C)
          .clearOrder()
          .withVisitNumber(3)
          .arriveAimedExpected("00:00:40", "00:00:45")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertFailure(MIXED_CALL_ORDER_AND_VISIT_NUMBER, result);
  }
}
