package org.opentripplanner.updater.trip.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import de.mfdz.MfdzRealtimeExtensions.StopTimePropertiesExtension.DropOffPickupType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.RealtimeTestEnvironment;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

class AddedTest implements RealtimeTestConstants {

  final String ADDED_TRIP_ID = "added_trip";

  @Test
  void addedTrip() {
    var env = RealtimeTestEnvironment.of().build();

    var tripUpdate = new TripUpdateBuilder(ADDED_TRIP_ID, SERVICE_DATE, ADDED, TIME_ZONE)
      .addStopTime(STOP_A1_ID, 30)
      .addStopTime(STOP_B1_ID, 40)
      .addStopTime(STOP_C1_ID, 55)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));
    assertAddedTrip(this.ADDED_TRIP_ID, env);
  }

  @Test
  void addedTripWithNewRoute() {
    var env = RealtimeTestEnvironment.of().build();
    var tripUpdate = new TripUpdateBuilder(ADDED_TRIP_ID, SERVICE_DATE, ADDED, TIME_ZONE)
      .addTripExtension()
      .addStopTime(STOP_A1_ID, 30, DropOffPickupType.PHONE_AGENCY)
      .addStopTime(STOP_B1_ID, 40, DropOffPickupType.COORDINATE_WITH_DRIVER)
      .addStopTime(STOP_B1_ID, 55, DropOffPickupType.NONE)
      .build();

    var result = env.applyTripUpdate(tripUpdate);
    assertSuccess(result);
    assertTrue(result.warnings().isEmpty());

    var pattern = assertAddedTrip(ADDED_TRIP_ID, env);

    var route = pattern.getRoute();
    assertEquals(TripUpdateBuilder.ROUTE_URL, route.getUrl());
    assertEquals(TripUpdateBuilder.ROUTE_NAME, route.getName());
    assertEquals(TransitMode.RAIL, route.getMode());

    TransitService transitService = env.getTransitService();
    var fromTimetableRepository = transitService.getRoute(route.getId());
    assertEquals(fromTimetableRepository, route);
    var patternsForRoute = transitService.findPatterns(route);
    assertEquals(1, patternsForRoute.size());
    assertEquals(pattern, patternsForRoute.stream().findFirst().orElseThrow());

    assertEquals(PickDrop.CALL_AGENCY, pattern.getBoardType(0));
    assertEquals(PickDrop.CALL_AGENCY, pattern.getAlightType(0));

    assertEquals(PickDrop.COORDINATE_WITH_DRIVER, pattern.getBoardType(1));
    assertEquals(PickDrop.COORDINATE_WITH_DRIVER, pattern.getAlightType(1));
  }

  @Test
  void addedWithUnknownStop() {
    var env = RealtimeTestEnvironment.of().build();
    var tripUpdate = new TripUpdateBuilder(ADDED_TRIP_ID, SERVICE_DATE, ADDED, TIME_ZONE)
      // add extension to set route name, url, mode
      .addTripExtension()
      .addStopTime(STOP_A1_ID, 30, DropOffPickupType.PHONE_AGENCY)
      .addStopTime("UNKNOWN_STOP_ID", 40, DropOffPickupType.COORDINATE_WITH_DRIVER)
      .addStopTime(STOP_C1_ID, 55, DropOffPickupType.NONE)
      .build();

    var result = env.applyTripUpdate(tripUpdate);
    assertSuccess(result);

    assertEquals(
      List.of(UpdateSuccess.WarningType.UNKNOWN_STOPS_REMOVED_FROM_ADDED_TRIP),
      result.warnings()
    );

    var pattern = assertAddedTrip(ADDED_TRIP_ID, env);

    assertEquals(2, pattern.getStops().size());
  }

  @Test
  void repeatedlyAddedTripWithNewRoute() {
    var env = RealtimeTestEnvironment.of().build();
    var tripUpdate = new TripUpdateBuilder(ADDED_TRIP_ID, SERVICE_DATE, ADDED, TIME_ZONE)
      // add extension to set route name, url, mode
      .addTripExtension()
      .addStopTime(STOP_A1_ID, 30, DropOffPickupType.PHONE_AGENCY)
      .addStopTime(STOP_B1_ID, 40, DropOffPickupType.COORDINATE_WITH_DRIVER)
      .addStopTime(STOP_C1_ID, 55, DropOffPickupType.NONE)
      .build();

    assertSuccess(env.applyTripUpdate(tripUpdate));
    var pattern = assertAddedTrip(ADDED_TRIP_ID, env);
    var firstRoute = pattern.getRoute();

    // apply the update a second time to check that no new route instance is created but the old one is reused
    env.applyTripUpdate(tripUpdate);
    var secondPattern = assertAddedTrip(ADDED_TRIP_ID, env);
    var secondRoute = secondPattern.getRoute();

    assertSame(firstRoute, secondRoute);
    assertNotNull(env.getTransitService().getRoute(firstRoute.getId()));
  }

  private TripPattern assertAddedTrip(String tripId, RealtimeTestEnvironment env) {
    var snapshot = env.getTimetableSnapshot();

    TransitService transitService = env.getTransitService();
    Trip trip = transitService.getTrip(TimetableRepositoryForTest.id(ADDED_TRIP_ID));
    assertNotNull(trip);
    assertNotNull(transitService.findPattern(trip));
    assertNotNull(
      transitService.getTripOnServiceDate(TimetableRepositoryForTest.id(ADDED_TRIP_ID))
    );

    var stopA = env.timetableRepository.getSiteRepository().getRegularStop(STOP_A1.getId());
    // Get the trip pattern of the added trip which goes through stopA
    var patternsAtA = env.getTimetableSnapshot().getPatternsForStop(stopA);

    assertNotNull(patternsAtA, "Added trip pattern should be found");
    assertEquals(1, patternsAtA.size());
    var tripPattern = patternsAtA.stream().findFirst().get();

    var forToday = snapshot.resolve(tripPattern, SERVICE_DATE);
    var schedule = snapshot.resolve(tripPattern, null);

    assertNotSame(forToday, schedule);

    final int forTodayAddedTripIndex = forToday.getTripIndex(tripId);
    assertTrue(
      forTodayAddedTripIndex > -1,
      "Added trip should be found in time table for service date"
    );
    assertEquals(
      RealTimeState.ADDED,
      forToday.getTripTimes(forTodayAddedTripIndex).getRealTimeState()
    );

    final int scheduleTripIndex = schedule.getTripIndex(tripId);
    assertEquals(-1, scheduleTripIndex, "Added trip should not be found in scheduled time table");
    return tripPattern;
  }
}
