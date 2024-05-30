package org.opentripplanner.ext.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.trip.RealtimeTestData;

class SiriTimetableSnapshotSourceTest {

  @Test
  void testCancelTrip() {
    var env = new SiriRealtimeTestEnvironment();

    assertEquals(
      RealTimeState.SCHEDULED,
      env.getTripTimesForTrip(env.testData.trip1).getRealTimeState()
    );

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.testData.trip1.getId().getId())
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      RealTimeState.CANCELED,
      env.getTripTimesForTrip(env.testData.trip1).getRealTimeState()
    );
  }

  @Test
  void testAddJourney() {
    var env = new SiriRealtimeTestEnvironment();
    var testData = env.testData;

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      .withOperatorRef(testData.operator1Id.getId())
      .withLineRef(testData.route1Id.getId())
      .withRecordedCalls(builder ->
        builder.call(testData.stopC1).departAimedActual("00:01", "00:02")
      )
      .withEstimatedCalls(builder ->
        builder.call(testData.stopD1).arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    var tripTimes = env.getTripTimesForTrip("newJourney");
    assertEquals(RealTimeState.ADDED, tripTimes.getRealTimeState());
    assertEquals(2 * 60, tripTimes.getDepartureTime(0));
    assertEquals(4 * 60, tripTimes.getDepartureTime(1));
  }

  @Test
  void testReplaceJourney() {
    var env = new SiriRealtimeTestEnvironment();
    var testData = env.testData;

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      // replace trip1
      .withVehicleJourneyRef(testData.trip1.getId().getId())
      .withOperatorRef(testData.operator1Id.getId())
      .withLineRef(testData.route1Id.getId())
      .withRecordedCalls(builder ->
        builder.call(testData.stopA1).departAimedActual("00:01", "00:02")
      )
      .withEstimatedCalls(builder ->
        builder.call(testData.stopC1).arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var tripTimes = env.getTripTimesForTrip("newJourney");
    var pattern = env.getPatternForTrip(testData.id("newJourney"));
    assertEquals(RealTimeState.ADDED, tripTimes.getRealTimeState());
    assertEquals(2 * 60, tripTimes.getDepartureTime(0));
    assertEquals(4 * 60, tripTimes.getDepartureTime(1));
    assertEquals(testData.stopA1.getId(), pattern.getStop(0).getId());
    assertEquals(testData.stopC1.getId(), pattern.getStop(1).getId());

    var originalTripTimes = env.getTripTimesForTrip(testData.trip1);
    assertEquals(RealTimeState.SCHEDULED, originalTripTimes.getRealTimeState());
  }

  /**
   * Update calls without changing the pattern. Match trip by dated vehicle journey.
   */
  @Test
  void testUpdateJourneyWithDatedVehicleJourneyRef() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = updatedJourneyBuilder(env)
      .withDatedVehicleJourneyRef(env.testData.trip1.getId().getId())
      .buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
  }

  /**
   * Update calls without changing the pattern. Match trip by framed vehicle journey.
   */
  @Test
  void testUpdateJourneyWithFramedVehicleJourneyRef() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = updatedJourneyBuilder(env)
      .withFramedVehicleJourneyRef(builder ->
        builder
          .withServiceDate(RealtimeTestData.SERVICE_DATE)
          .withVehicleJourneyRef(env.testData.trip1.getId().getId())
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
    var env = new SiriRealtimeTestEnvironment();

    var updates = updatedJourneyBuilder(env).buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(0, result.successful());
    assertFailure(result, UpdateError.UpdateErrorType.TRIP_NOT_FOUND);
  }

  /**
   * Update calls without changing the pattern. Fuzzy matching.
   */
  @Test
  void testUpdateJourneyWithFuzzyMatching() {
    var env = new SiriRealtimeTestEnvironment();

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
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(RealtimeTestData.SERVICE_DATE).withVehicleJourneyRef("XXX")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(env.testData.stopA1)
          .departAimedExpected(null, "00:00:12")
          .call(env.testData.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:22")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetableWithFuzzyMatcher(updates);
    assertEquals(0, result.successful(), "Should fail gracefully");
    assertFailure(result, UpdateError.UpdateErrorType.NO_FUZZY_TRIP_MATCH);
  }

  /**
   * Change quay on a trip
   */
  @Test
  void testChangeQuay() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.testData.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder.call(env.testData.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder.call(env.testData.stopB2).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var pattern = env.getPatternForTrip(env.testData.trip1.getId());
    var tripTimes = env.getTripTimesForTrip(env.testData.trip1);
    assertEquals(RealTimeState.MODIFIED, tripTimes.getRealTimeState());
    assertEquals(11, tripTimes.getScheduledDepartureTime(0));
    assertEquals(15, tripTimes.getDepartureTime(0));
    assertEquals(20, tripTimes.getScheduledArrivalTime(1));
    assertEquals(33, tripTimes.getArrivalTime(1));
    assertEquals(env.testData.stopB2, pattern.getStop(1));
  }

  @Test
  void testCancelStop() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.testData.trip2.getId().getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.testData.stopA1)
          .departAimedExpected("00:01:01", "00:01:01")
          .call(env.testData.stopB1)
          .withIsCancellation(true)
          .call(env.testData.stopC1)
          .arriveAimedExpected("00:01:30", "00:01:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var pattern = env.getPatternForTrip(env.testData.trip2.getId());

    assertEquals(PickDrop.SCHEDULED, pattern.getAlightType(0));
    assertEquals(PickDrop.CANCELLED, pattern.getAlightType(1));
    assertEquals(PickDrop.SCHEDULED, pattern.getAlightType(2));
  }

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testAddStop() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.testData.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder.call(env.testData.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(env.testData.stopD1)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(env.testData.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var pattern = env.getPatternForTrip(env.testData.trip1.getId());
    var tripTimes = env.getTripTimesForTrip(env.testData.trip1);
    assertEquals(RealTimeState.MODIFIED, tripTimes.getRealTimeState());
    assertEquals(11, tripTimes.getScheduledDepartureTime(0));
    assertEquals(15, tripTimes.getDepartureTime(0));

    // Should it work to get the scheduled times from an extra call?
    assertEquals(19, tripTimes.getScheduledArrivalTime(1));
    assertEquals(24, tripTimes.getScheduledDepartureTime(1));

    assertEquals(20, tripTimes.getDepartureTime(1));
    assertEquals(25, tripTimes.getDepartureTime(1));

    assertEquals(20, tripTimes.getScheduledArrivalTime(2));
    assertEquals(33, tripTimes.getArrivalTime(2));
    assertEquals(
      List.of(env.testData.stopA1, env.testData.stopD1, env.testData.stopB1),
      pattern.getStops()
    );
  }

  /////////////////
  // Error cases //
  /////////////////

  @Test
  void testNotMonitored() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withMonitored(false)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(result, UpdateError.UpdateErrorType.NOT_MONITORED);
  }

  @Test
  void testReplaceJourneyWithoutEstimatedVehicleJourneyCode() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef("newJourney")
      .withIsExtraJourney(true)
      .withVehicleJourneyRef(env.testData.trip1.getId().getId())
      .withOperatorRef(env.testData.operator1Id.getId())
      .withLineRef(env.testData.route1Id.getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.testData.stopA1)
          .departAimedExpected("00:01", "00:02")
          .call(env.testData.stopC1)
          .arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    // TODO: this should have a more specific error type
    assertFailure(result, UpdateError.UpdateErrorType.UNKNOWN);
  }

  @Test
  void testNegativeHopTime() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.testData.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder
          .call(env.testData.stopA1)
          .departAimedActual("00:00:11", "00:00:15")
          .call(env.testData.stopB1)
          .arriveAimedActual("00:00:20", "00:00:14")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(result, UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME);
  }

  @Test
  void testNegativeDwellTime() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.testData.trip2.getId().getId())
      .withRecordedCalls(builder ->
        builder
          .call(env.testData.stopA1)
          .departAimedActual("00:01:01", "00:01:01")
          .call(env.testData.stopB1)
          .arriveAimedActual("00:01:10", "00:01:13")
          .departAimedActual("00:01:11", "00:01:12")
          .call(env.testData.stopB1)
          .arriveAimedActual("00:01:20", "00:01:20")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(result, UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME);
  }

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testExtraUnknownStop() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.testData.trip1.getId().getId())
      .withEstimatedCalls(builder ->
        builder
          .call(env.testData.stopA1)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected extra stop without isExtraCall flag
          .call(env.testData.stopD1)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(env.testData.stopB1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(result, UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE);
  }

  private void assertFailure(UpdateResult result, UpdateError.UpdateErrorType errorType) {
    assertEquals(result.failures().keySet(), Set.of(errorType));
  }

  private static SiriEtBuilder updatedJourneyBuilder(SiriRealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper())
      .withRecordedCalls(builder ->
        builder.call(env.testData.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder.call(env.testData.stopB1).arriveAimedExpected("00:00:20", "00:00:25")
      );
  }

  private static void assertTripUpdated(SiriRealtimeTestEnvironment env) {
    var tripTimes = env.getTripTimesForTrip(env.testData.trip1);
    assertEquals(RealTimeState.UPDATED, tripTimes.getRealTimeState());
    assertEquals(11, tripTimes.getScheduledDepartureTime(0));
    assertEquals(15, tripTimes.getDepartureTime(0));
    assertEquals(20, tripTimes.getScheduledArrivalTime(1));
    assertEquals(25, tripTimes.getArrivalTime(1));
  }
}
