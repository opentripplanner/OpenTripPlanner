package org.opentripplanner.updater.trip.siri.moduletests.cancellation;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class CancellationTest implements RealtimeTestConstants {

  private static final String ADDED_TRIP_ID = "newJourney";
  private static final String ROUTE_ID = "route-id";
  private static final String OPERATOR_ID = "operator-id";

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stopAtStation(STOP_B_ID, STATION_OMEGA_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stopAtStation(STOP_D_ID, STATION_OMEGA_ID);
  private final Operator OPERATOR = ENV_BUILDER.operator(OPERATOR_ID);
  private final Route ROUTE = ENV_BUILDER.route(ROUTE_ID, OPERATOR);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .withRoute(ROUTE)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  @Test
  void testCancelTrip() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    assertEquals(RealTimeState.SCHEDULED, env.tripData(TRIP_1_ID).realTimeState());

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withCancellation(true)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .arriveAimedExpected("0:00:10", "0:00:10")
          .departAimedExpected("0:00:11", "0:00:11")
          .call(STOP_B)
          .arriveAimedExpected("0:00:20", "0:00:20")
          .departAimedExpected("0:00:21", "0:00:21")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(RealTimeState.CANCELED, env.tripData(TRIP_1_ID).realTimeState());
  }

  @Test
  void testCancelTripWithMissingTimes() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    assertEquals(RealTimeState.SCHEDULED, env.tripData(TRIP_1_ID).realTimeState());

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(RealTimeState.CANCELED, env.tripData(TRIP_1_ID).realTimeState());
  }

  /**
   * When a scheduled trip is modified (both trip times and stops) and subsequently cancelled,
   * it should be marked as cancelled and reverted to its scheduled trip times and stops.
   */
  @Test
  void testChangeQuayAndCancelScheduledTrip() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    assertEquals(
      "SCHEDULED | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.tripData(TRIP_1_ID).showTimetable()
    );
    changeQuayAndCancelTrip(siri, TRIP_1_ID);

    assertEquals(
      "CANCELED | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * When an added trip is modified (both trip times and stops) and subsequently cancelled,
   * it should be marked as cancelled and reverted to its initial trip times and stops
   */
  @Test
  void testChangeQuayAndCancelAddedTrip() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();

    assertThat(env.raptorData().summarizePatterns()).containsExactly("F:Pattern1[SCHEDULED]");
    var siri = SiriTestHelper.of(env);

    var creation = new SiriEtBuilder(env.localTimeParser())
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .arriveAimedExpected("0:00:10", "0:00:10")
          .departAimedExpected("0:00:11", "0:00:11")
          .call(STOP_B)
          .arriveAimedExpected("0:00:20", "0:00:20")
          .departAimedExpected("0:00:21", "0:00:21")
      )
      .buildEstimatedTimetableDeliveries();
    var creationResult = siri.applyEstimatedTimetable(creation);
    assertSuccess(creationResult);
    assertEquals(
      "ADDED | A 0:00:10 0:00:11 | B 0:00:20 0:00:21",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:route-id::001:RT[ADDED]"
    );

    changeQuayAndCancelTrip(siri, ADDED_TRIP_ID);

    // the arrival time on first stop is adjusted to the departure time to avoid negative dwell time
    // conversely the departure time on last stop is adjusted to the arrival time
    assertEquals(
      "CANCELED | A 0:00:11 0:00:11 | B 0:00:20 0:00:20",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertThat(env.raptorData().summarizePatterns()).containsExactly(
      "F:Pattern1[SCHEDULED]",
      "F:route-id::001:RT[CANCELED]"
    );
  }

  private void changeQuayAndCancelTrip(SiriTestHelper siri, String tripId) {
    var modification = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(tripId)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          // change to another quay in the same station
          .call(STOP_C)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var modificationResult = siri.applyEstimatedTimetable(modification);
    assertSuccess(modificationResult);
    TransitTestEnvironment transitTestEnvironment = siri.realtimeTestEnvironment();
    assertEquals(
      "MODIFIED | A 0:00:15 0:00:15 | D 0:00:25 0:00:25",
      transitTestEnvironment.tripData(tripId).showTimetable()
    );

    var cancellation = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(tripId)
      .withCancellation(true)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          // change to another quay in the same station
          .call(STOP_C)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var cancellationResult = siri.applyEstimatedTimetable(cancellation);

    assertSuccess(cancellationResult);
  }
}
