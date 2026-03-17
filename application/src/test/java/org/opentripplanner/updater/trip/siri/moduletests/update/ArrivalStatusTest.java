package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;
import uk.org.siri.siri21.CallStatusEnumeration;

class ArrivalStatusTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:10", "0:10")
    .addStop(STOP_B, "0:20", "0:20")
    .addStop(STOP_C, "0:30", "0:30");

  /// Verify that a RecordedCall with ActualArrivalTime but no ActualDepartureTime
  /// is treated as arrived but NOT departed. This matches the SIRI-ET semantics where
  /// a vehicle can be at the platform (arrived) but not yet have left (not departed).
  @Test
  void testRecordedCallWithArrivalButNoDeparture() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);
    var update = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A)
          .arriveAimedActual("00:10", "00:10")
          .departAimedExpected("00:10", "00:11")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_B)
          .arriveAimedExpected("00:20", "00:20")
          .departAimedExpected("00:20", "00:20")
          .call(STOP_C)
          .arriveAimedExpected("00:30", "00:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(update);
    assertSuccess(result);

    var tt = env.tripData(TRIP_1_ID).tripTimes();
    assertTrue(tt.hasArrived(0));
    assertFalse(tt.hasDeparted(0));
    assertFalse(tt.hasArrived(1));
    assertFalse(tt.hasDeparted(1));
    assertFalse(tt.hasArrived(2));
    assertFalse(tt.hasDeparted(2));
  }

  /// Verify handling of ArrivalStatus == ARRIVED
  @Test
  void testUpdateJourneyWithArrivalStatusArrived() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);
    var update = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:10", "00:10"))
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_B)
          .departAimedExpected("00:20", "00:20")
          .withArrivalStatus(CallStatusEnumeration.ARRIVED)
          .call(STOP_C)
          .arriveAimedExpected("00:30", "00:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(update);
    assertSuccess(result);

    var tt = env.tripData(TRIP_1_ID).tripTimes();
    assertTrue(tt.hasArrived(0));
    assertTrue(tt.hasDeparted(0));
    assertTrue(tt.hasArrived(1));
    assertFalse(tt.hasDeparted(1));
    assertFalse(tt.hasArrived(2));
    assertFalse(tt.hasDeparted(2));
  }
}
