package org.opentripplanner.updater.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripInput;

class SiriRealTimeTripUpdateAdapterTest implements RealtimeTestConstants {

  private static final TripInput TRIP_1_INPUT = TripInput
    .of(TRIP_1_ID)
    .withRoute(ROUTE_1.copy().withOperator(OPERATOR1).build())
    .addStop(STOP_A1, "0:00:10", "0:00:11")
    .addStop(STOP_B1, "0:00:20", "0:00:21")
    .build();

  private static final TripInput TRIP_2_INPUT = TripInput
    .of(TRIP_2_ID)
    .addStop(STOP_A1, "0:01:00", "0:01:01")
    .addStop(STOP_B1, "0:01:10", "0:01:11")
    .addStop(STOP_C1, "0:01:20", "0:01:21")
    .build();

  @Test
  void testCancelTrip() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    assertEquals(RealTimeState.SCHEDULED, env.getTripTimesForTrip(TRIP_1_ID).getRealTimeState());

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withCancellation(true)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(RealTimeState.CANCELED, env.getTripTimesForTrip(TRIP_1_ID).getRealTimeState());
  }

  @Test
  void testAddJourneyWithExistingRoute() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    Route route = ROUTE_1;
    int numPatternForRoute = env.getTransitService().findPatterns(route).size();

    String newJourneyId = "newJourney";
    var updates = createValidAddedJourney(env).buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals("ADDED | C1 [R] 0:02 0:02 | D1 0:04 0:04", env.getRealtimeTimetable(newJourneyId));
    assertEquals(
      "SCHEDULED | C1 0:01 0:01 | D1 0:03 0:03",
      env.getScheduledTimetable(newJourneyId)
    );
    FeedScopedId tripId = id(newJourneyId);
    TransitService transitService = env.getTransitService();
    Trip trip = transitService.getTrip(tripId);
    assertNotNull(trip);
    assertNotNull(transitService.findPattern(trip));
    assertNotNull(transitService.getTripOnServiceDate(tripId));
    assertNotNull(
      transitService.getTripOnServiceDate(new TripIdAndServiceDate(tripId, SERVICE_DATE))
    );
    assertEquals(
      numPatternForRoute + 1,
      transitService.findPatterns(route).size(),
      "The added trip should use a new pattern for this route"
    );
  }

  @Test
  void testAddJourneyWithNewRoute() {
    // we actually don't need the trip, but it's the only way to add a route to the index
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    String newRouteRef = "new route ref";
    var updates = createValidAddedJourney(env)
      .withLineRef(newRouteRef)
      .buildEstimatedTimetableDeliveries();

    int numRoutes = env.getTransitService().listRoutes().size();
    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals("ADDED | C1 [R] 0:02 0:02 | D1 0:04 0:04", env.getRealtimeTimetable("newJourney"));
    assertEquals(
      "SCHEDULED | C1 0:01 0:01 | D1 0:03 0:03",
      env.getScheduledTimetable("newJourney")
    );
    TransitService transitService = env.getTransitService();
    assertEquals(numRoutes + 1, transitService.listRoutes().size());
    FeedScopedId newRouteId = id(newRouteRef);
    Route newRoute = transitService.getRoute(newRouteId);
    assertNotNull(newRoute);
    assertEquals(1, transitService.findPatterns(newRoute).size());
  }

  @Test
  void testAddJourneyMultipleTimes() {
    // we actually don't need the trip, but it's the only way to add a route to the index
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();
    var updates = createValidAddedJourney(env).buildEstimatedTimetableDeliveries();

    int numTrips = env.getTransitService().listTrips().size();
    var result1 = env.applyEstimatedTimetable(updates);
    assertEquals(1, result1.successful());
    assertEquals(numTrips + 1, env.getTransitService().listTrips().size());
    var result2 = env.applyEstimatedTimetable(updates);
    assertEquals(1, result2.successful());
    assertEquals(numTrips + 1, env.getTransitService().listTrips().size());
  }

  @Test
  void testAddedJourneyWithInvalidScheduledData() {
    // we actually don't need the trip, but it's the only way to add a route to the index
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    // Create an extra journey with invalid planned data (travel back in time)
    // and valid real time data
    var createExtraJourney = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_1_ID)
      .withLineRef(ROUTE_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("10:58", "10:48")
          .call(STOP_B1)
          .arriveAimedExpected("10:08", "10:58")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(createExtraJourney);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testAddedJourneyWithUnresolvableAgency() {
    var env = RealtimeTestEnvironment.of().build();

    // Create an extra journey with unknown line and operator
    var createExtraJourney = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      .withOperatorRef("unknown operator")
      .withLineRef("unknown line")
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("10:58", "10:48")
          .call(STOP_B1)
          .arriveAimedExpected("10:08", "10:58")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(createExtraJourney);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.CANNOT_RESOLVE_AGENCY, result);
  }

  @Test
  void testReplaceJourney() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      // replace trip1
      .withVehicleJourneyRef(TRIP_1_ID)
      .withOperatorRef(OPERATOR_1_ID)
      .withLineRef(ROUTE_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A1).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(STOP_C1).arriveAimedExpected("00:03", "00:04"))
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    assertEquals("ADDED | A1 [R] 0:02 0:02 | C1 0:04 0:04", env.getRealtimeTimetable("newJourney"));
    assertEquals(
      "SCHEDULED | A1 0:01 0:01 | C1 0:03 0:03",
      env.getScheduledTimetable("newJourney")
    );

    // Original trip should not get canceled
    var originalTripTimes = env.getTripTimesForTrip(TRIP_1_ID);
    assertEquals(RealTimeState.SCHEDULED, originalTripTimes.getRealTimeState());
  }

  /**
   * Update calls without changing the pattern. Match trip by dated vehicle journey.
   */
  @Test
  void testUpdateJourneyWithDatedVehicleJourneyRef() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = updatedJourneyBuilder(env)
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .buildEstimatedTimetableDeliveries();
    var result = env.applyEstimatedTimetable(updates);
    assertEquals(1, result.successful());
    assertTripUpdated(env);
    assertEquals(
      "UPDATED | A1 0:00:15 0:00:15 | B1 0:00:25 0:00:25",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  /**
   * Update calls without changing the pattern. Match trip by framed vehicle journey.
   */
  @Test
  void testUpdateJourneyWithFramedVehicleJourneyRef() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = updatedJourneyBuilder(env)
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(SERVICE_DATE).withVehicleJourneyRef(TRIP_1_ID)
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
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

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
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

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
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withFramedVehicleJourneyRef(builder ->
        builder.withServiceDate(SERVICE_DATE).withVehicleJourneyRef("XXX")
      )
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected(null, "00:00:12")
          .call(STOP_B1)
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
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A1).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder.call(STOP_B2).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 [R] 0:00:15 0:00:15 | B2 0:00:33 0:00:33",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  @Test
  void testCancelStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_2_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_2_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:01:01", "00:01:01")
          .call(STOP_B1)
          .withIsCancellation(true)
          .call(STOP_C1)
          .arriveAimedExpected("00:01:30", "00:01:30")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 0:01:01 0:01:01 | B1 [C] 0:01:10 0:01:11 | C1 0:01:30 0:01:30",
      env.getRealtimeTimetable(TRIP_2_ID)
    );
  }

  // TODO: support this
  @Test
  @Disabled("Not supported yet")
  void testAddStop() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A1).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_D1)
          .withIsExtraCall(true)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "MODIFIED | A1 0:00:15 0:00:15 | D1 [C] 0:00:20 0:00:25 | B1 0:00:33 0:00:33",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }

  /////////////////
  // Error cases //
  /////////////////

  @Test
  void testNotMonitored() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withMonitored(false)
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NOT_MONITORED, result);
  }

  @Test
  void testReplaceJourneyWithoutEstimatedVehicleJourneyCode() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef("newJourney")
      .withIsExtraJourney(true)
      .withVehicleJourneyRef(TRIP_1_ID)
      .withOperatorRef(OPERATOR_1_ID)
      .withLineRef(ROUTE_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:01", "00:02")
          .call(STOP_C1)
          .arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    // TODO: this should have a more specific error type
    assertFailure(UpdateError.UpdateErrorType.UNKNOWN, result);
  }

  @Test
  void testNegativeHopTime() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedActual("00:00:11", "00:00:15")
          .call(STOP_B1)
          .arriveAimedActual("00:00:20", "00:00:14")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testNegativeDwellTime() {
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_2_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_2_ID)
      .withRecordedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedActual("00:01:01", "00:01:01")
          .call(STOP_B1)
          .arriveAimedActual("00:01:10", "00:01:13")
          .departAimedActual("00:01:11", "00:01:12")
          .call(STOP_B1)
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
    var env = RealtimeTestEnvironment.of().addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          // Unexpected extra stop without isExtraCall flag
          .call(STOP_D1)
          .arriveAimedExpected("00:00:19", "00:00:20")
          .departAimedExpected("00:00:24", "00:00:25")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertFailure(UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE, result);
  }

  private static SiriEtBuilder createValidAddedJourney(RealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode("newJourney")
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_1_ID)
      .withLineRef(ROUTE_1_ID)
      .withRecordedCalls(builder -> builder.call(STOP_C1).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(STOP_D1).arriveAimedExpected("00:03", "00:04"));
  }

  private static SiriEtBuilder updatedJourneyBuilder(RealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A1)
          .departAimedExpected("00:00:11", "00:00:15")
          .call(STOP_B1)
          .arriveAimedExpected("00:00:20", "00:00:25")
      );
  }

  private static void assertTripUpdated(RealtimeTestEnvironment env) {
    assertEquals(
      "UPDATED | A1 0:00:15 0:00:15 | B1 0:00:25 0:00:25",
      env.getRealtimeTimetable(TRIP_1_ID)
    );
  }
}
