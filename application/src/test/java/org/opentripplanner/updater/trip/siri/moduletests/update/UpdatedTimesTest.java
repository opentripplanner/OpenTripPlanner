package org.opentripplanner.updater.trip.siri.moduletests.update;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.SiriTestHelper;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;
import uk.org.siri.siri21.OccupancyEnumeration;

class UpdatedTimesTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);

  private final TripInput TRIP_INPUT = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  /**
   * Update calls without changing the pattern. Match trip by dated vehicle journey.
   */
  @Test
  void testUpdateJourneyWithDatedVehicleJourneyRef() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updatedJourneyBuilder(siri)
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .buildEstimatedTimetableDeliveries();
    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);
    assertTripUpdated(env);
    assertEquals(
      "UPDATED | A 0:00:15 0:00:15 | B 0:00:25 0:00:25",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * Update calls without changing the pattern. Match trip by framed vehicle journey.
   */
  @Test
  void testUpdateJourneyWithFramedVehicleJourneyRef() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updatedJourneyBuilder(siri)
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(env.defaultServiceDate()).withVehicleJourneyRef(TRIP_1_ID)
      )
      .buildEstimatedTimetableDeliveries();
    var result = siri.applyEstimatedTimetable(updates);
    assertSuccess(result);
    assertTripUpdated(env);
  }

  /**
   * Update calls without changing the pattern. Missing reference to vehicle journey.
   */
  @Test
  void testUpdateJourneyWithoutJourneyRef() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = updatedJourneyBuilder(siri).buildEstimatedTimetableDeliveries();
    var result = siri.applyEstimatedTimetable(updates);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.TRIP_NOT_FOUND, result);
  }

  /**
   * Update a 3-stop trip with a mix of recorded calls (past stops) and estimated calls
   * (future stops).
   */
  @Test
  void testUpdateJourneyWithRecordedAndEstimatedCalls() {
    var threeStopTrip = TripInput.of(TRIP_1_ID)
      .withWithTripOnServiceDate(TRIP_1_ID)
      .addStop(STOP_A, "0:00:10", "0:00:11")
      .addStop(STOP_B, "0:00:20", "0:00:21")
      .addStop(STOP_C, "0:00:30", "0:00:31");

    var env = ENV_BUILDER.addTrip(threeStopTrip).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
          .departAimedExpected("00:00:21", "00:00:26")
          .call(STOP_C)
          .arriveAimedExpected("00:00:30", "00:00:35")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "UPDATED | A [R] 0:00:15 0:00:15 | B 0:00:25 0:00:26 | C 0:00:35 0:00:35",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * Apply two successive updates to the same trip. The second update should replace the first,
   * not accumulate delays.
   */
  @Test
  void testUpdateJourneyMultipleTimes() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // First update: small delay
    var firstUpdate = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result1 = siri.applyEstimatedTimetable(firstUpdate);
    assertSuccess(result1);
    assertEquals(
      "UPDATED | A 0:00:15 0:00:15 | B 0:00:25 0:00:25",
      env.tripData(TRIP_1_ID).showTimetable()
    );

    // Second update: larger delay replaces the first
    var secondUpdate = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:20")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result2 = siri.applyEstimatedTimetable(secondUpdate);
    assertSuccess(result2);
    assertEquals(
      "UPDATED | A 0:00:20 0:00:20 | B 0:00:33 0:00:33",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  /**
   * Set journey-level occupancy and verify it propagates to all stops.
   */
  @Test
  void testUpdateJourneyWithOccupancy() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withOccupancy(OccupancyEnumeration.SEATS_AVAILABLE)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    var tripTimes = env.tripData(TRIP_1_ID).tripTimes();
    assertEquals(OccupancyStatus.MANY_SEATS_AVAILABLE, tripTimes.getOccupancyStatus(0));
    assertEquals(OccupancyStatus.MANY_SEATS_AVAILABLE, tripTimes.getOccupancyStatus(1));
  }

  /**
   * Set prediction inaccurate flag and verify it appears on all stops.
   */
  @Test
  void testUpdateJourneyWithPredictionInaccurate() {
    var env = ENV_BUILDER.addTrip(TRIP_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withPredictionInaccurate(true)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertSuccess(result);
    assertEquals(
      "UPDATED | A [PI] 0:00:15 0:00:15 | B [PI] 0:00:25 0:00:25",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }

  private SiriEtBuilder updatedJourneyBuilder(SiriTestHelper siri) {
    return siri
      .etBuilder()
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B)
          .arriveAimedExpected("00:00:20", "00:00:25")
      );
  }

  private static void assertTripUpdated(TransitTestEnvironment env) {
    assertEquals(
      "UPDATED | A 0:00:15 0:00:15 | B 0:00:25 0:00:25",
      env.tripData(TRIP_1_ID).showTimetable()
    );
  }
}
