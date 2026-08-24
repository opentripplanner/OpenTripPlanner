package org.opentripplanner.updater.trip.siri.moduletests.extracall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

class ExtraCallScheduledStopTest implements RealtimeTestConstants {

  private final String ROUTE_ID = "route-id";

  private final LocalDate DATE_1 = LocalDate.of(2022, 2, 22);
  private final LocalDate DATE_2 = DATE_1.plusDays(1);

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stopAtStation(STOP_A_ID, "A");
  private final RegularStop STOP_B = ENV_BUILDER.stopAtStation(STOP_B_ID, "B");
  private final RegularStop STOP_E = ENV_BUILDER.stopAtStation(STOP_E_ID, "E");
  private final Route ROUTE = ENV_BUILDER.route(ROUTE_ID);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withServiceDates(DATE_1, DATE_2)
    .withRoute(ROUTE)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  @Test
  void testScheduledStopOnExtraCall() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder(DATE_1)
      .withFramedVehicleJourneyRef(TRIP_1_ID, DATE_1)
      .withLineRef(ROUTE_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_E)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:18", "00:00:20")
          .departAimedExpected("00:00:19", "00:00:25")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    // The trip should be the same on the two days
    assertEquals(env.tripData(TRIP_1_ID, DATE_1).trip(), env.tripData(TRIP_1_ID, DATE_2).trip());
    assertEquals(
      "P U | A 0:00:15 0:00:15 | E [EC] 0:00:20 0:00:25 | B 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID, DATE_1).showTimetable()
    );
    assertEquals(
      "S | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.tripData(TRIP_1_ID, DATE_2).showTimetable()
    );

    // Verify that we set the correct scheduled stops
    var trip = env.tripData(TRIP_1_ID, DATE_1).trip();
    var scheduledPattern = env.transitService().findPattern(trip);
    var tt1 = env.transitService().findTripTimesOnDate(trip, DATE_1).orElseThrow();
    assertEquals("A", tt1.get(0).getScheduledStop(scheduledPattern).getId().getId());
    assertEquals("E", tt1.get(1).getScheduledStop(scheduledPattern).getId().getId());
    assertEquals("B", tt1.get(2).getScheduledStop(scheduledPattern).getId().getId());

    var tt2 = env.transitService().findTripTimesOnDate(trip, DATE_2).orElseThrow();
    assertEquals("A", tt2.get(0).getScheduledStop(scheduledPattern).getId().getId());
    assertEquals("B", tt2.get(1).getScheduledStop(scheduledPattern).getId().getId());
  }
}
