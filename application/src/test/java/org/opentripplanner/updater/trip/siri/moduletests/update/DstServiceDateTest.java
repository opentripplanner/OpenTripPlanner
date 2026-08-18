package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.updater.trip.OnEachDstTransitionAndAControlDate;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

class DstServiceDateTest implements RealtimeTestConstants {

  /** An updated trip keeps the times the message reports on a daylight-saving transition date. */
  @OnEachDstTransitionAndAControlDate
  void updatedTimesOnDstServiceDate(String name, LocalDate serviceDate) {
    var envBuilder = TransitTestEnvironment.of(serviceDate);
    var stopA = envBuilder.stop(STOP_A_ID);
    var stopB = envBuilder.stop(STOP_B_ID);
    var env = envBuilder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .withWithTripOnServiceDate(TRIP_1_ID)
          .addStop(stopA, "10:00", "10:01")
          .addStop(stopB, "10:20", "10:21")
      )
      .build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(stopA).departAimedActual("10:01", "10:05"))
      .withEstimatedCalls(builder -> builder.call(stopB).arriveAimedExpected("10:20", "10:33"))
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertEquals("U | A [R] 10:05 10:05 | B 10:33 10:33", env.tripData(TRIP_1_ID).showTimetable());
  }
}
