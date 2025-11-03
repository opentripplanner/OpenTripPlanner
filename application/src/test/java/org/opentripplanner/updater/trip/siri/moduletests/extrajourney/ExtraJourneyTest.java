package org.opentripplanner.updater.trip.siri.moduletests.extrajourney;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertFailure;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
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
import org.opentripplanner.updater.trip.SiriTestHelper;
import org.opentripplanner.updater.trip.siri.SiriEtBuilder;

class ExtraJourneyTest implements RealtimeTestConstants {

  private static final String ADDED_TRIP_ID = "newJourney";
  private static final String OPERATOR_ID = "operatorId";
  private static final String ROUTE_ID = "routeId";

  private final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of();
  private final RegularStop STOP_A = ENV_BUILDER.stop(STOP_A_ID);
  private final RegularStop STOP_B = ENV_BUILDER.stop(STOP_B_ID);
  private final RegularStop STOP_C = ENV_BUILDER.stop(STOP_C_ID);
  private final RegularStop STOP_D = ENV_BUILDER.stop(STOP_D_ID);

  private final Operator OPERATOR = ENV_BUILDER.operator(OPERATOR_ID);
  private final Route ROUTE = ENV_BUILDER.route(ROUTE_ID, OPERATOR);

  private final TripInput TRIP_1_INPUT = TripInput.of(TRIP_1_ID)
    .withRoute(ROUTE)
    .addStop(STOP_A, "0:00:10", "0:00:11")
    .addStop(STOP_B, "0:00:20", "0:00:21");

  @Test
  void testAddJourneyWithExistingRoute() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    Route route = ROUTE;
    int numPatternForRoute = env.transitService().findPatterns(route).size();

    var updates = createValidAddedJourney(siri).buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "ADDED | C [R] 0:02 0:02 | D 0:04 0:04",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertEquals(
      "SCHEDULED | C 0:01 0:01 | D 0:03 0:03",
      env.tripData(ADDED_TRIP_ID).showScheduledTimetable()
    );
    FeedScopedId tripId = id(ADDED_TRIP_ID);
    TransitService transitService = env.transitService();
    Trip trip = transitService.getTrip(tripId);
    assertNotNull(trip);
    assertNotNull(transitService.findPattern(trip));
    assertNotNull(transitService.getTripOnServiceDate(tripId));
    assertNotNull(
      transitService.getTripOnServiceDate(
        new TripIdAndServiceDate(tripId, env.defaultServiceDate())
      )
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
    var siri = SiriTestHelper.of(env);

    String newRouteRef = "new route ref";
    var updates = createValidAddedJourney(siri)
      .withLineRef(newRouteRef)
      .buildEstimatedTimetableDeliveries();

    int numRoutes = env.transitService().listRoutes().size();
    var result = siri.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());
    assertEquals(
      "ADDED | C [R] 0:02 0:02 | D 0:04 0:04",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertEquals(
      "SCHEDULED | C 0:01 0:01 | D 0:03 0:03",
      env.tripData(ADDED_TRIP_ID).showScheduledTimetable()
    );
    TransitService transitService = env.transitService();
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
    var siri = SiriTestHelper.of(env);

    var updates = createValidAddedJourney(siri).buildEstimatedTimetableDeliveries();

    int numTrips = env.transitService().listTrips().size();
    var result1 = siri.applyEstimatedTimetable(updates);
    assertEquals(1, result1.successful());
    assertEquals(numTrips + 1, env.transitService().listTrips().size());
    var result2 = siri.applyEstimatedTimetable(updates);
    assertEquals(1, result2.successful());
    assertEquals(numTrips + 1, env.transitService().listTrips().size());
  }

  @Test
  void testAddedJourneyWithInvalidScheduledData() {
    // we actually don't need the trip, but it's the only way to add a route to the index
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    // Create an extra journey with invalid planned data (travel back in time)
    // and valid real time data
    var createExtraJourney = siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("10:58", "10:48")
          .call(STOP_B)
          .arriveAimedExpected("10:08", "10:58")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(createExtraJourney);
    assertEquals(0, result.successful());
    assertFailure(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, result);
  }

  @Test
  void testReplaceJourney() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      // replace trip1
      .withVehicleJourneyRef(TRIP_1_ID)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withRecordedCalls(builder -> builder.call(STOP_A).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(STOP_C).arriveAimedExpected("00:03", "00:04"))
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    assertEquals(1, result.successful());

    assertEquals(
      "ADDED | A [R] 0:02 0:02 | C 0:04 0:04",
      env.tripData(ADDED_TRIP_ID).showTimetable()
    );
    assertEquals(
      "SCHEDULED | A 0:01 0:01 | C 0:03 0:03",
      env.tripData(ADDED_TRIP_ID).showScheduledTimetable()
    );

    // Original trip should not get canceled
    var originalTripTimes = env.tripData(TRIP_1_ID).tripTimes();
    assertEquals(RealTimeState.SCHEDULED, originalTripTimes.getRealTimeState());
  }

  @Test
  void testReplaceJourneyWithoutEstimatedVehicleJourneyCode() {
    var env = ENV_BUILDER.addTrip(TRIP_1_INPUT).build();
    var siri = SiriTestHelper.of(env);

    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withVehicleJourneyRef(TRIP_1_ID)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withEstimatedCalls(builder ->
        builder
          .call(STOP_A)
          .departAimedExpected("00:01", "00:02")
          .call(STOP_C)
          .arriveAimedExpected("00:03", "00:04")
      )
      .buildEstimatedTimetableDeliveries();

    var result = siri.applyEstimatedTimetable(updates);

    // TODO: this should have a more specific error type
    assertFailure(UpdateError.UpdateErrorType.UNKNOWN, result);
  }

  private SiriEtBuilder createValidAddedJourney(SiriTestHelper siri) {
    return siri
      .etBuilder()
      .withEstimatedVehicleJourneyCode(ADDED_TRIP_ID)
      .withIsExtraJourney(true)
      .withOperatorRef(OPERATOR_ID)
      .withLineRef(ROUTE_ID)
      .withRecordedCalls(builder -> builder.call(STOP_C).departAimedActual("00:01", "00:02"))
      .withEstimatedCalls(builder -> builder.call(STOP_D).arriveAimedExpected("00:03", "00:04"));
  }
}
