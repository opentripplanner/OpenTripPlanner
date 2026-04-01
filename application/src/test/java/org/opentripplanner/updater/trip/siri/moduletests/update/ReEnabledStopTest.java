package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;
import uk.org.siri.siri21.DepartureBoardingActivityEnumeration;

class ReEnabledStopTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  /**
   * A SIRI update with DepartureBoardingActivity=BOARDING should re-enable a stop
   * that was scheduled with PickDrop.NONE (no boarding). This must produce a MODIFIED
   * trip (new pattern) because the stop's pickup type changed from NONE to SCHEDULED.
   */
  @Test
  void boardingActivityReEnablesStopWithPickDropNone() {
    var tripInput = TripInput.of(TRIP_1_ID)
      .withWithTripOnServiceDate(TRIP_1_ID)
      .addStop(STOP_A, "0:00:10", "0:00:11")
      .addStop(STOP_B, "0:00:20", "0:00:21", PickDrop.NONE, PickDrop.SCHEDULED)
      .addStop(STOP_C, "0:00:30", "0:00:31");

    var env = ENV_BUILDER.addTrip(tripInput).build();
    var siri = SiriTestHelper.of(env);

    // Verify the scheduled pattern has NONE pickup at stop B
    var scheduledPattern = env.tripData(TRIP_1_ID).scheduledTripPattern();
    assertEquals(PickDrop.NONE, scheduledPattern.getBoardType(1));

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:11")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:20")
          .departAimedExpected("00:00:21", "00:00:21")
          .withDepartureBoardingActivity(DepartureBoardingActivityEnumeration.BOARDING)
          .call(STOP_C)
          .arriveAimedExpected("00:00:30", "00:00:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);

    // The trip should be MODIFIED (new pattern) because stop B's pickup changed NONE -> SCHEDULED
    assertEquals(
      "MODIFIED | A 0:00:11 0:00:11 | B 0:00:20 0:00:21 | C 0:00:30 0:00:30",
      env.tripData(TRIP_1_ID).showTimetable()
    );

    // Verify the new pattern has SCHEDULED pickup at stop B
    var realtimePattern = env.tripData(TRIP_1_ID).tripPattern();
    assertEquals(PickDrop.SCHEDULED, realtimePattern.getBoardType(1));
  }
}
