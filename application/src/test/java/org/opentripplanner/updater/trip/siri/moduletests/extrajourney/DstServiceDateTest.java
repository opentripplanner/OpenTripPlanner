package org.opentripplanner.updater.trip.siri.moduletests.extrajourney;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.updater.trip.OnEachDstTransitionAndAControlDate;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

class DstServiceDateTest implements RealtimeTestConstants {

  private static final String ADDED_TRIP_ID = "newJourney";
  private static final String OPERATOR_ID = "operatorId";
  private static final String ROUTE_ID = "routeId";

  /** An extra journey keeps the times the message reports on a daylight-saving transition date. */
  @OnEachDstTransitionAndAControlDate
  void extraJourneyOnDstServiceDate(String name, LocalDate serviceDate) {
    var envBuilder = TransitTestEnvironment.of(serviceDate);
    var stopA = envBuilder.stop(STOP_A_ID);
    var stopB = envBuilder.stop(STOP_B_ID);
    var operator = envBuilder.operator(OPERATOR_ID);
    var route = envBuilder.route(ROUTE_ID, operator);
    // a scheduled trip is what puts the service date in the calendar
    var env = envBuilder
      .addTrip(
        TripInput.of(TRIP_1_ID).withRoute(route).addStop(stopA, "12:00").addStop(stopB, "12:10")
      )
      .build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withRecordedCalls(builder -> builder.call(stopA).departAimedActual("10:01", "10:02"))
      .withEstimatedCalls(builder -> builder.call(stopB).arriveAimedExpected("10:03", "10:04"))
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertEquals(
      "A U | A [R] 10:02 10:02 | B 10:04 10:04",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertEquals(
      "S | A 10:01 10:01 | B 10:03 10:03",
      env.tripData(ADDED_TRIP_ID).showScheduledTimetable()
    );
  }
}
