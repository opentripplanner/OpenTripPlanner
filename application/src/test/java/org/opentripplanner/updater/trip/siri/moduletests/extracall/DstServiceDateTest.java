package org.opentripplanner.updater.trip.siri.moduletests.extracall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.updater.trip.OnEachDstTransitionAndAControlDate;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

class DstServiceDateTest implements RealtimeTestConstants {

  private static final String ROUTE_ID = "route-id";

  /** An extra call keeps the times the message reports on a daylight-saving transition date. */
  @OnEachDstTransitionAndAControlDate
  void extraCallOnDstServiceDate(String name, LocalDate serviceDate) {
    var envBuilder = TransitTestEnvironment.of(serviceDate);
    var stopA = envBuilder.stopAtStation(STOP_A_ID, "A");
    var stopB = envBuilder.stopAtStation(STOP_B_ID, "B");
    var stopC = envBuilder.stopAtStation(STOP_C_ID, "C");
    var route = envBuilder.route(ROUTE_ID);
    var env = envBuilder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .withWithTripOnServiceDate(TRIP_1_ID)
          .withRoute(route)
          .addStop(stopA, "10:00", "10:01")
          .addStop(stopB, "10:20", "10:21")
      )
      .build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withLineRef(ROUTE_ID)
      .withRecordedCalls(builder -> builder.call(stopA).departAimedActual("10:01", "10:05"))
      .withEstimatedCalls(builder ->
        builder
          .call(stopC)
          .withIsExtraCall(true)
          .arriveAimedExpected("10:08", "10:10")
          .departAimedExpected("10:09", "10:15")
          .call(stopB)
          .arriveAimedExpected("10:20", "10:33")
      )
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertEquals(
      "P U | A [R] 10:05 10:05 | C [EC] 10:10 10:15 | B 10:33 10:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }
}
