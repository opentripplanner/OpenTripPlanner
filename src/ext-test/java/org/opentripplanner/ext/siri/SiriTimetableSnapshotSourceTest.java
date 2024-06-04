package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.StopModel;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;

class SiriTimetableSnapshotSourceTest {

  @Test
  void testCancelTrip() {
    var env = RealtimeTestEnvironment.siri();

    assertEquals(RealTimeState.SCHEDULED, env.getTripTimesForTrip(env.trip1).getRealTimeState());

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(RealTimeState.CANCELED, env.getTripTimesForTrip(env.trip1).getRealTimeState());
  }

  @Test
  void testAddJourney() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      .withOperatorRef(env.operator1Id.getId())
      .withLineRef(env.route1Id.getId())
      .withRecordedCalls(builder -> builder.call(env.stopC1).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(env.stopD1).arriveAimedExpected("00:03", "00:04"))
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals("ADDED | C1 [R] 0:02 0:02 | D1 0:04 0:04", env.getRealtimeTimetable("newJourney"));
    assertEquals(
      "SCHEDULED | C1 0:01 0:01 | D1 0:03 0:03",
      env.getScheduledTimetable("newJourney")
    );
  }

  @Test
  void testAddedJourneyWithInvalidScheduledData() {
    var env = RealtimeTestEnvironment.siri();

    // Create an extra journey with invalid planned data (travel back in time)
    // and valid real time data
    var createExtraJourney = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      .withOperatorRef(env.operator1Id.getId())
      .withLineRef(env.route1Id.getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected("10:58", "10:48")
          .call(env.stopB1)
          .arriveAimedExpected("10:08", "10:58")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(createExtraJourney);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testReplaceJourney() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      // replace trip1
      .withVehicleJourneyRef(env.trip1.getId().getId())
      .withOperatorRef(env.operator1Id.getId())
      .withLineRef(env.route1Id.getId())
      .withRecordedCalls(builder -> builder.call(env.stopA1).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(env.stopC1).arriveAimedExpected("00:03", "00:04"))
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    assertEquals("ADDED | A1 [R] 0:02 0:02 | C1 0:04 0:04", env.getRealtimeTimetable("newJourney"));
    assertEquals(
      "SCHEDULED | A1 0:01 0:01 | C1 0:03 0:03",
      env.getScheduledTimetable("newJourney")
    );

    // Original trip should not get canceled
    var originalTripTimes = env.getTripTimesForTrip(env.trip1);
    assertEquals(RealTimeState.SCHEDULED, originalTripTimes.getRealTimeState());
  }

  /**
   * Update calls without changing the pattern. Match trip by dated vehicle journey.
   */
  @Test
  void testUpdateJourneyWithDatedVehicleJourneyRef() {
    var env = RealtimeTestEnvironment.siri();

    var updates = updatedJourneyBuilder(env)
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
    assertEquals(
      "UPDATED | A1 0:00:15 0:00:15 | B1 0:00:25 0:00:25",
      env.getRealtimeTimetable(env.trip1)
    );
  }

  /**
   * Update calls without changing the pattern. Match trip by framed vehicle journey.
   */
  @Test
  void testUpdateJourneyWithFramedVehicleJourneyRef() {
    var env = RealtimeTestEnvironment.siri();

    var updates = updatedJourneyBuilder(env)
      .withFramedVehicleJourneyRef(builder ->
        builder
          .withServiceDate(RealtimeTestEnvironment.SERVICE_DATE)
          .withVehicleJourneyRef(env.trip1.getId().getId())
      )
      .buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
  }

  /**
   * Update calls without changing the pattern. Missing reference to vehicle journey.
   */
  @Test
  void testUpdateJourneyWithoutJourneyRef() {
    var env = RealtimeTestEnvironment.siri();

    var updates = updatedJourneyBuilder(env).buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.TRIP_NOT_FOUND, result);
  }

  /**
   * Update calls without changing the pattern. Fuzzy matching.
   */
  @Test
  void testUpdateJourneyWithFuzzyMatching() {
    var env = RealtimeTestEnvironment.siri();

    var updates = updatedJourneyBuilder(env).buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetableWithFuzzyMatcher(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
  }

  /**
   * Update calls without changing the pattern. Fuzzy matching.
   * Edge case: invalid reference to vehicle journey and missing aimed departure time.
   */
  @Test
  void testUpdateJourneyWithFuzzyMatchingAndMissingAimedDepartureTime() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(RealtimeTestEnvironment.SERVICE_DATE).withVehicleJourneyRef("XXX")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected(null, "00:00:12")
          .call(env.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:22")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetableWithFuzzyMatcher(updates);
    assertEquals(0, result.successful(), "Should fail gracefully");
    assertFailure(UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH, result);
  }

  /**
   * Change quay on a trip
   */
  @Test
  void testChangeQuay() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder.call(env.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder.call(env.stopB2).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 [R] 0:00:15 0:00:15 | B2 0:00:33 0:00:33",
      env.getRealtimeTimetable(env.trip1)
    );
  }

  @Test
  void testCancelStop() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip2.getId().getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected("00:01:01", "00:01:01")
          .call(env.stopB1)
          .withIsCancellation(true)
          .call(env.stopC1)
          .arriveAimedExpected("00:01:30", "00:01:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 0:01:01 0:01:01 | B1 [C] 0:01:10 0:01:11 | C1 0:01:30 0:01:30",
      env.getRealtimeTimetable(env.trip2)
    );
  }

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testAddStop() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder.call(env.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopD1)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(env.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 0:00:15 0:00:15 | D1 [C] 0:00:20 0:00:25 | B1 0:00:33 0:00:33",
      env.getRealtimeTimetable(env.trip1)
    );
  }

  /////////////////
  // Error cases //
  /////////////////

  @Test
  void testNotMonitored() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withMonitored(false)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NOT_MONITORED, result);
  }

  @Test
  void testReplaceJourneyWithoutEstimatedVehicleJourneyCode() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef("newJourney")
      .withIsExtraJourney(true)
      .withVehicleJourneyRef(env.trip1.getId().getId())
      .withOperatorRef(env.operator1Id.getId())
      .withLineRef(env.route1Id.getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected("00:01", "00:02")
          .call(env.stopC1)
          .arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    // TODO: this should have a more specific error type
    assertFailure(UpdateError.UpdateErrorType.UNKNOWN, result);
  }

  @Test
  void testNegativeHopTime() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedActual("00:00:11", "00:00:15")
          .call(env.stopB1)
          .arriveAimedActual("00:00:20", "00:00:14")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testNegativeDwellTime() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip2.getId().getId())
      .withRecordedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedActual("00:01:01", "00:01:01")
          .call(env.stopB1)
          .arriveAimedActual("00:01:10", "00:01:13")
          .departAimedActual("00:01:11", "00:01:12")
          .call(env.stopB1)
          .arriveAimedActual("00:01:20", "00:01:20")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME, result);
  }

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testExtraUnknownStop() {
    var env = RealtimeTestEnvironment.siri();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected extra stop without isExtraCall flag
          .call(env.stopD1)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(env.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE, result);
  }

  private void assertFailure(UpdateError.UpdateErrorType expectedError, UpdateResult result) {
    assertEquals(Set.of(expectedError), result.failures().keySet());
  }

  private static SiriEtBuilder updatedJourneyBuilder(RealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedCalls(builder ->
        builder
          .call(env.stopA1)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(env.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:25")
      );
  }

  private static void assertTripUpdated(RealtimeTestEnvironment env) {
    assertEquals(
      "UPDATED | A1 0:00:15 0:00:15 | B1 0:00:25 0:00:25",
      env.getRealtimeTimetable(env.trip1)
    );
  }
}
