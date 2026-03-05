package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.updater.spi.UpdateError.UpdateErrorType;
import uk.org.siri.siri21.EstimatedCall;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.RecordedCall;
import uk.org.siri.siri21.StopPointRefStructure;

class CallWrapperParsingTest {

  @Test
  void parseValidEstimatedCallsWithOrder() {
    var journey = new EstimatedVehicleJourney();
    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_A", 1, null));
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_B", 2, null));
    journey.setEstimatedCalls(estimatedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isSuccess());
    var calls = result.successValue();
    assertEquals(2, calls.size());
    assertEquals("STOP_A", calls.get(0).getStopPointRef());
    assertEquals(1, calls.get(0).getSortOrder());
    assertEquals("STOP_B", calls.get(1).getStopPointRef());
    assertEquals(2, calls.get(1).getSortOrder());
  }

  @Test
  void parseValidCallsWithVisitNumber() {
    var journey = new EstimatedVehicleJourney();
    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_A", null, 1));
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_B", null, 2));
    journey.setEstimatedCalls(estimatedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isSuccess());
    var calls = result.successValue();
    assertEquals(2, calls.size());
    assertEquals(1, calls.get(0).getSortOrder());
    assertEquals(2, calls.get(1).getSortOrder());
  }

  @Test
  void rejectMissingOrderAndVisitNumber() {
    var journey = new EstimatedVehicleJourney();
    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_A", null, null));
    journey.setEstimatedCalls(estimatedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isFailure());
    assertEquals(UpdateErrorType.MISSING_CALL_ORDER, result.failureValue());
  }

  @Test
  void rejectBothOrderAndVisitNumber() {
    var journey = new EstimatedVehicleJourney();
    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_A", 1, 1));
    journey.setEstimatedCalls(estimatedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isFailure());
    assertEquals(UpdateErrorType.MIXED_CALL_ORDER_AND_VISIT_NUMBER, result.failureValue());
  }

  @Test
  void rejectMixedOrderAndVisitNumberAcrossCalls() {
    var journey = new EstimatedVehicleJourney();
    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_A", 1, null));
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_B", null, 2));
    journey.setEstimatedCalls(estimatedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isFailure());
    assertEquals(UpdateErrorType.MIXED_CALL_ORDER_AND_VISIT_NUMBER, result.failureValue());
  }

  @Test
  void rejectEmptyStopPointRef() {
    var journey = new EstimatedVehicleJourney();
    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall(null, 1, null));
    journey.setEstimatedCalls(estimatedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isFailure());
    assertEquals(UpdateErrorType.EMPTY_STOP_POINT_REF, result.failureValue());
  }

  @Test
  void sortByOrder() {
    var journey = new EstimatedVehicleJourney();
    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_C", 3, null));
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_A", 1, null));
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_B", 2, null));
    journey.setEstimatedCalls(estimatedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isSuccess());
    var calls = result.successValue();
    assertEquals(
      List.of("STOP_A", "STOP_B", "STOP_C"),
      calls.stream().map(CallWrapper::getStopPointRef).toList()
    );
  }

  @Test
  void parseMixedRecordedAndEstimatedCalls() {
    var journey = new EstimatedVehicleJourney();

    var recordedCalls = new EstimatedVehicleJourney.RecordedCalls();
    recordedCalls.getRecordedCalls().add(recordedCall("STOP_A", 1, null));
    journey.setRecordedCalls(recordedCalls);

    var estimatedCalls = new EstimatedVehicleJourney.EstimatedCalls();
    estimatedCalls.getEstimatedCalls().add(estimatedCall("STOP_B", 2, null));
    journey.setEstimatedCalls(estimatedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isSuccess());
    var calls = result.successValue();
    assertEquals(2, calls.size());
    assertTrue(calls.get(0).isRecorded());
    assertEquals("STOP_A", calls.get(0).getStopPointRef());
    assertEquals(false, calls.get(1).isRecorded());
    assertEquals("STOP_B", calls.get(1).getStopPointRef());
  }

  @Test
  void parseEmptyJourney() {
    var journey = new EstimatedVehicleJourney();

    var result = CallWrapper.of(journey);

    assertTrue(result.isSuccess());
    assertTrue(result.successValue().isEmpty());
  }

  @Test
  void rejectEmptyStopPointRefOnRecordedCall() {
    var journey = new EstimatedVehicleJourney();
    var recordedCalls = new EstimatedVehicleJourney.RecordedCalls();
    recordedCalls.getRecordedCalls().add(recordedCall(null, 1, null));
    journey.setRecordedCalls(recordedCalls);

    var result = CallWrapper.of(journey);

    assertTrue(result.isFailure());
    assertEquals(UpdateErrorType.EMPTY_STOP_POINT_REF, result.failureValue());
  }

  private static EstimatedCall estimatedCall(String stopRef, Integer order, Integer visitNumber) {
    var call = new EstimatedCall();
    if (stopRef != null) {
      var ref = new StopPointRefStructure();
      ref.setValue(stopRef);
      call.setStopPointRef(ref);
    }
    if (order != null) {
      call.setOrder(BigInteger.valueOf(order));
    }
    if (visitNumber != null) {
      call.setVisitNumber(BigInteger.valueOf(visitNumber));
    }
    return call;
  }

  private static RecordedCall recordedCall(String stopRef, Integer order, Integer visitNumber) {
    var call = new RecordedCall();
    if (stopRef != null) {
      var ref = new StopPointRefStructure();
      ref.setValue(stopRef);
      call.setStopPointRef(ref);
    }
    if (order != null) {
      call.setOrder(BigInteger.valueOf(order));
    }
    if (visitNumber != null) {
      call.setVisitNumber(BigInteger.valueOf(visitNumber));
    }
    return call;
  }
}
