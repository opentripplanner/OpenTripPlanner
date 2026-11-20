package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;
import uk.org.siri.siri21.CallStatusEnumeration;

class NotExpectedStatusTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:10", "0:10")
    .addStop(STOP_B, "0:20", "0:20")
    .addStop(STOP_C, "0:30", "0:30");

  /// Verify handling of Arrival and Departure Status == NOT_EXPECTED
  @Test
  void testUpdateJourneyWithArrivalStatusNotExpected() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);
    var update = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:10", "00:10")
          .withDepartureStatus(CallStatusEnumeration.NOT_EXPECTED)
          .call(STOP_B)
          .departAimedExpected("00:20", "00:20")
          .withArrivalStatus(CallStatusEnumeration.NOT_EXPECTED)
          .call(STOP_C)
          .arriveAimedExpected("00:30", "00:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(update);
    assertSuccess(result);

    var tt = env.tripData(TRIP_1_ID).tripTimes();
    assertTrue(tt.isNotExpectedDeparture(0));
    assertFalse(tt.isNotExpectedArrival(0));
    assertTrue(tt.isNotExpectedArrival(1));
    assertFalse(tt.isNotExpectedDeparture(1));
  }
}
