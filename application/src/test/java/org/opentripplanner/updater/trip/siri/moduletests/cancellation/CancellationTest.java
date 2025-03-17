package org.opentripplanner.updater.trip.siri.moduletests.cancellation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class CancellationTest implements RealtimeTestConstants {

  private static final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .build();

  private static final String ADDED_TRIP_ID = "newJourney";

  @Test
  void testCancelTrip() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();

    assertEquals(RealTimeState.SCHEDULED, env.getTripTimesForTrip(TRIP_1_ID).getRealTimeState());

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withCancellation(true)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .arriveAimedExpected("0:00:10", "0:00:10")
          .departAimedExpected("0:00:11", "0:00:11")
          .call(STOP_B1)
          .arriveAimedExpected("0:00:20", "0:00:20")
          .departAimedExpected("0:00:21", "0:00:21")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(RealTimeState.CANCELED, env.getTripTimesForTrip(TRIP_1_ID).getRealTimeState());
  }

  @Test
  void testCancelTripWithMissingTimes() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();
    assertEquals(RealTimeState.SCHEDULED, env.getTripTimesForTrip(TRIP_1_ID).getRealTimeState());
    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(RealTimeState.CANCELED, env.getTripTimesForTrip(TRIP_1_ID).getRealTimeState());
  }

  /**
   * When a scheduled trip is modified (both trip times and stops) and subsequently cancelled,
   * it should be marked as cancelled and reverted to its scheduled trip times and stops.
   */
  @Test
  void testChangeQuayAndCancelScheduledTrip() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();
    assertEquals(
      "SCHEDULED | A1 0:00:10 0:00:11 | B1 0:00:20 0:00:21",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
    changeQuayAndCancelTrip(env, TRIP_1_ID);

    assertEquals(
      "CANCELED | A1 0:00:10 0:00:11 | B1 0:00:20 0:00:21",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  /**
   * When an added trip is modified (both trip times and stops) and subsequently cancelled,
   * it should be marked as cancelled and reverted to its initial trip times and stops
   */
  @Test
  void testChangeQuayAndCancelAddedTrip() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_INPUT).build();
    var creation = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_1_ID)
      .withLineRef(ROUTE_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .arriveAimedExpected("0:00:10", "0:00:10")
          .departAimedExpected("0:00:11", "0:00:11")
          .call(STOP_B1)
          .arriveAimedExpected("0:00:20", "0:00:20")
          .departAimedExpected("0:00:21", "0:00:21")
      )
      .buildEstimatedTimetableDeliveries();
    var creationResult = env.applyEstimatedTimetable(creation);
    assertEquals(1, creationResult.successful());
    assertEquals(
      "ADDED | A1 0:00:10 0:00:11 | B1 0:00:20 0:00:21",
      env.getRealtimeTimetable(ADDED_TRIP_ID)
    );
    changeQuayAndCancelTrip(env, ADDED_TRIP_ID);

    // the arrival time on first stop is adjusted to the departure time to avoid negative dwell time
    // conversely the departure time on last stop is adjusted to the arrival time
    assertEquals(
      "CANCELED | A1 0:00:11 0:00:11 | B1 0:00:20 0:00:20",
      env.getRealtimeTimetable(ADDED_TRIP_ID)
    );
  }

  private static void changeQuayAndCancelTrip(RealtimeTestEnvironment env, String tripId) {
    var modification = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(tripId)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          // change to another quay in the same station
          .call(STOP_B2)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var modificationResult = env.applyEstimatedTimetable(modification);
    assertEquals(1, modificationResult.successful());
    assertEquals(
      "MODIFIED | A1 0:00:15 0:00:15 | B2 0:00:25 0:00:25",
      env.getRealtimeTimetable(tripId)
    );

    var cancellation = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(tripId)
      .withCancellation(true)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          // change to another quay in the same station
          .call(STOP_B2)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var cancellationResult = env.applyEstimatedTimetable(cancellation);

    assertEquals(1, cancellationResult.successful());
  }
}
