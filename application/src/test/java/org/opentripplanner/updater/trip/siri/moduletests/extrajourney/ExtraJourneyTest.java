package org.opentripplanner.updater.trip.siri.moduletests.extrajourney;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripIdAndServiceDate;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.RealtimeTestEnvironmentBuilder;
import org.opentripplanner.updater.trip.TripInput;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class ExtraJourneyTest implements RealtimeTestConstants {

  private static final String ADDED_TRIP_ID = "newJourney";
  private static final Route ROUTE_2 = TimetableRepositoryForTest.route("route-2")
    .withOperator(Operator.of(id("o2")).withName("o").build())
    .build();

  private final RealtimeTestEnvironmentBuilder ENV_BUILDER = RealtimeTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withRoute(ROUTE_2)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21")
    .build();

  @Test
  void testAddJourneyWithExistingRoute() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    Route route = ROUTE_2;
    int numPatternForRoute = env.getTransitService().findPatterns(route).size();

    var updates = createValidAddedJourney(env).buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals("ADDED | C [R] 0:02 0:02 | D 0:04 0:04", env.getRealtimeTimetable(ADDED_TRIP_ID));
    assertEquals("SCHEDULED | C 0:01 0:01 | D 0:03 0:03", env.getScheduledTimetable(ADDED_TRIP_ID));
    FeedScopedId tripId = id(ADDED_TRIP_ID);
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
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    String newRouteRef = "new route ref";
    var updates = createValidAddedJourney(env)
      .withLineRef(newRouteRef)
      .buildEstimatedTimetableDeliveries();

    int numRoutes = env.getTransitService().listRoutes().size();
    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals("ADDED | C [R] 0:02 0:02 | D 0:04 0:04", env.getRealtimeTimetable(ADDED_TRIP_ID));
    assertEquals("SCHEDULED | C 0:01 0:01 | D 0:03 0:03", env.getScheduledTimetable(ADDED_TRIP_ID));
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
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
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
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    // Create an extra journey with invalid planned data (travel back in time)
    // and valid real time data
    var createExtraJourney = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(TRIP_1_INPUT.operatorId())
      .withLineRef(TRIP_1_INPUT.routeId())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("10:58", "10:48")
          .call(STOP_B)
          .arriveAimedExpected("10:08", "10:58")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(createExtraJourney);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testReplaceJourney() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      // replace trip1
      .withVehicleJourneyRef(TRIP_1_ID)
      .withOperatorRef(TRIP_1_INPUT.operatorId())
      .withLineRef(TRIP_1_INPUT.routeId())
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(STOP_C).arriveAimedExpected("00:03", "00:04"))
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    assertEquals("ADDED | A [R] 0:02 0:02 | C 0:04 0:04", env.getRealtimeTimetable(ADDED_TRIP_ID));
    assertEquals("SCHEDULED | A 0:01 0:01 | C 0:03 0:03", env.getScheduledTimetable(ADDED_TRIP_ID));

    // Original trip should not get canceled
    var originalTripTimes = env.getTripTimesForTrip(TRIP_1_ID);
    assertEquals(RealTimeState.SCHEDULED, originalTripTimes.getRealTimeState());
  }

  @Test
  void testReplaceJourneyWithoutEstimatedVehicleJourneyCode() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();

    var updates = new SiriEtBuilder(env.getDateTimeHelper())
      .withDatedVehicleJourneyRef(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withVehicleJourneyRef(TRIP_1_ID)
      .withOperatorRef(TRIP_1_INPUT.operatorId())
      .withLineRef(TRIP_1_INPUT.routeId())
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01", "00:02")
          .call(STOP_C)
          .arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = env.applyEstimatedTimetable(updates);

    // TODO: this should have a more specific error type
    assertFailure(UpdateError.UpdateErrorType.UNKNOWN, result);
  }

  private SiriEtBuilder createValidAddedJourney(RealtimeTestEnvironment env) {
    return new SiriEtBuilder(env.getDateTimeHelper())
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(TRIP_1_INPUT.operatorId())
      .withLineRef(TRIP_1_INPUT.routeId())
      .withRecordedCalls(builder -> builder.call(STOP_C).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(STOP_D).arriveAimedExpected("00:03", "00:04"));
  }
}
