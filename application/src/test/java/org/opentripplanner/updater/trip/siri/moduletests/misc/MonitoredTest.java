package org.opentripplanner.updater.trip.siri.moduletests.misc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

class MonitoredTest implements RealtimeTestConstants {

  private static final String OPERATOR_ID = "operatorId";
  private static final String ROUTE_ID = "routeId";

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);

  private final Operator OPERATOR = ENV_BUILDER.operator(OPERATOR_ID);
  private final Route ROUTE = ENV_BUILDER.route(ROUTE_ID, OPERATOR);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withRoute(ROUTE)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  @Test
  void testMonitoredUpdate() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updatedJourneyBuilder(siri)
      .withMonitored(true)
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertTrue(env.tripData(TRIP_1_ID).tripTimes().realtimeTripState().monitored());
  }

  @Test
  void testNotMonitoredUpdate() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updatedJourneyBuilder(siri)
      .withMonitored(false)
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertFalse(env.tripData(TRIP_1_ID).tripTimes().realtimeTripState().monitored());
  }

  @Test
  void testMonitoredCancellation() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updatedJourneyBuilder(siri)
      .withMonitored(true)
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertTrue(env.tripData(TRIP_1_ID).tripTimes().realtimeTripState().monitored());
  }

  @Test
  void testNotMonitoredCancellation() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updatedJourneyBuilder(siri)
      .withMonitored(false)
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertFalse(env.tripData(TRIP_1_ID).tripTimes().realtimeTripState().monitored());
  }

  @Test
  void testNotMonitoredAdded() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = addedJourneyBuilder(siri)
      .withMonitored(false)
      .buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertFalse(env.tripData(ADDED_TRIP_ID).tripTimes().realtimeTripState().monitored());
  }

  @Test
  void testMonitoredAdded() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = addedJourneyBuilder(siri).withMonitored(true).buildEstimatedTimetableDeliveries();

    assertSuccess(siri.applyEstimatedTimetable(updates));

    assertTrue(env.tripData(ADDED_TRIP_ID).tripTimes().realtimeTripState().monitored());
  }

  private SiriEtBuilder updatedJourneyBuilder(SiriTestHelper siri) {
    return siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
      );
  }

  private SiriEtBuilder addedJourneyBuilder(SiriTestHelper siri) {
    return siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:03", "00:04")
          .call(STOP_B)
          .arriveAimedExpected("00:05", "00:06")
      );
  }
}
