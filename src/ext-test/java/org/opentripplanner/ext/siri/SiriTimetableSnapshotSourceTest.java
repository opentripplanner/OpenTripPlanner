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

class SiriTimetableSnapshotSourceTest {

  @Test
  void testCancelTrip() {
    var env = new SiriRealtimeTestEnvironment();

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
    var env = new SiriRealtimeTestEnvironment();

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
    var tripTimes = env.getTripTimesForTrip("newJourney");
    assertEquals(RealTimeState.ADDED, tripTimes.getRealTimeState());
    assertEquals(2 * 60, tripTimes.getDepartureTime(0));
    assertEquals(4 * 60, tripTimes.getDepartureTime(1));
  }

  @Test
  void testReplaceJourney() {
    var env = new SiriRealtimeTestEnvironment();

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

    var tripTimes = env.getTripTimesForTrip("newJourney");
    var pattern = env.getPatternForTrip(env.id("newJourney"));
    assertEquals(RealTimeState.ADDED, tripTimes.getRealTimeState());
    assertEquals(2 * 60, tripTimes.getDepartureTime(0));
    assertEquals(4 * 60, tripTimes.getDepartureTime(1));
    assertEquals(env.stopA1.getId(), pattern.getStop(0).getId());
    assertEquals(env.stopC1.getId(), pattern.getStop(1).getId());

    var originalTripTimes = env.getTripTimesForTrip(env.trip1);
    assertEquals(RealTimeState.SCHEDULED, originalTripTimes.getRealTimeState());
  }

  /**
   * Update calls without changing the pattern
   */
  @Test
  void testUpdateJourney() {
    var env = new SiriRealtimeTestEnvironment();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(env.trip1.getId().getId())
      .withRecordedCalls(builder ->
        builder.call(env.stopA1).departAimedActual("00:00:11", "00:00:15")
      )
      .withEstimatedCalls(builder ->
        builder.call(env.stopB1).arriveAimedExpected("00:00:20", "00:00:25")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    var tripTimes = env.getTripTimesForTrip(env.trip1);
    assertEquals(RealTimeState.UPDATED, tripTimes.getRealTimeState());
    assertEquals(11, tripTimes.getScheduledDepartureTime(0));
    assertEquals(15, tripTimes.getDepartureTime(0));
    assertEquals(20, tripTimes.getScheduledArrivalTime(1));
    assertEquals(25, tripTimes.getArrivalTime(1));
  }

  /**
   * Change quay on a trip
   */
  @Test
  void testChangeQuay() {
    var env = new SiriRealtimeTestEnvironment();

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

    var pattern = env.getPatternForTrip(env.trip1.getId());
    var tripTimes = env.getTripTimesForTrip(env.trip1);
    assertEquals(RealTimeState.MODIFIED, tripTimes.getRealTimeState());
    assertEquals(11, tripTimes.getScheduledDepartureTime(0));
    assertEquals(15, tripTimes.getDepartureTime(0));
    assertEquals(20, tripTimes.getScheduledArrivalTime(1));
    assertEquals(33, tripTimes.getArrivalTime(1));
    assertEquals(env.stopB2, pattern.getStop(1));
  }

  @Test
  void testCancelStop() {
    var env = new SiriRealtimeTestEnvironment();

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

    var pattern = env.getPatternForTrip(env.trip2.getId());

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

    var pattern = env.getPatternForTrip(env.trip1.getId());
    var tripTimes = env.getTripTimesForTrip(env.trip1);
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
    assertEquals(List.of(env.stopA1, env.stopD1, env.stopB1), pattern.getStops());
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
    assertFailure(result, UpdateError.UpdateErrorType.UNKNOWN);
  }

  @Test
  void testNegativeHopTime() {
    var env = new SiriRealtimeTestEnvironment();

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

    assertFailure(result, UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME);
  }

  @Test
  void testNegativeDwellTime() {
    var env = new SiriRealtimeTestEnvironment();

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

    assertFailure(result, UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME);
  }

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testExtraUnknownStop() {
    var env = new SiriRealtimeTestEnvironment();

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

    assertFailure(result, UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE);
  }

  private void assertFailure(UpdateResult result, UpdateError.UpdateErrorType errorType) {
    assertEquals(result.failures().keySet(), Set.of(errorType));
  }
}
