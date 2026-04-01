package org.opentripplanner.updater.trip.gtfs.moduletests.addition;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
import static com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.StopTimeProperties.DropOffPickupType.COORDINATE_WITH_DRIVER;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model._data.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;
import static org.opentripplanner.updater.trip.UpdateIncrementality.DIFFERENTIAL;

import de.mfdz.MfdzRealtimeExtensions.StopTimePropertiesExtension.DropOffPickupType;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model._data.TransitTestEnvironment;
import org.opentripplanner.transit.model._data.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model._data.TripInput;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.GtfsRtTestHelper;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.TripUpdateBuilder;
import org.opentripplanner.utils.time.TimeUtils;

class AddedTest implements RealtimeTestConstants {

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = envBuilder.stop(STOP_C_ID);

  private final TransitTestEnvironment env = envBuilder
    .addTrip(
      TripInput.of(TRIP_1_ID)
        // just to set the schedule period
        .withServiceDates(
          envBuilder.defaultServiceDate().minusDays(1),
          envBuilder.defaultServiceDate().plusDays(1)
        )
        .addStop(STOP_A, "12:00", "12:00")
        .addStop(STOP_B, "12:10", "12:10")
        .addStop(STOP_C, "12:20", "12:20")
    )
    .addStops(STOP_A_ID, STOP_B_ID, STOP_C_ID, STOP_D_ID)
    .build();
  private final GtfsRtTestHelper gtfsRt = GtfsRtTestHelper.of(env);

  @Test
  void addedTrip() {
    var tripUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:40")
      .addStopTime(STOP_C_ID, "00:55")
      .build();

    assertSuccess(gtfsRt.applyTripUpdate(tripUpdate));
    assertAddedTrip(ADDED_TRIP_ID, env);
  }

  @Test
  void addedTripWithNewRoute() {
    var tripUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .addTripExtension()
      .addStopTime(STOP_A_ID, "00:30", DropOffPickupType.PHONE_AGENCY)
      .addStopTime(STOP_B_ID, "00:40", COORDINATE_WITH_DRIVER)
      .addStopTime(STOP_B_ID, "00:55", DropOffPickupType.NONE)
      .build();

    var result = gtfsRt.applyTripUpdate(tripUpdate);
    assertSuccess(result);
    assertTrue(result.warnings().isEmpty());

    var pattern = assertAddedTrip(ADDED_TRIP_ID, env);

    var route = pattern.getRoute();
    assertEquals(TripUpdateBuilder.ROUTE_URL, route.getUrl());
    assertEquals(TripUpdateBuilder.ROUTE_NAME, route.getName());
    assertEquals(TransitMode.RAIL, route.getMode());

    TransitService transitService = env.transitService();
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
    var tripUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      // add extension to set route name, url, mode
      .addTripExtension()
      .addStopTime(STOP_A_ID, "00:30", DropOffPickupType.PHONE_AGENCY)
      .addStopTime("UNKNOWN_STOP_ID", "00:40", DropOffPickupType.COORDINATE_WITH_DRIVER)
      .addStopTime(STOP_C_ID, "00:55", DropOffPickupType.NONE)
      .build();

    var result = gtfsRt.applyTripUpdate(tripUpdate);
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
    var tripUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      // add extension to set route name, url, mode
      .addTripExtension()
      .addStopTime(STOP_A_ID, "00:30", DropOffPickupType.PHONE_AGENCY)
      .addStopTime(STOP_B_ID, "00:40", DropOffPickupType.COORDINATE_WITH_DRIVER)
      .addStopTime(STOP_C_ID, "00:55", DropOffPickupType.NONE)
      .build();

    assertSuccess(gtfsRt.applyTripUpdate(tripUpdate));
    var pattern = assertAddedTrip(ADDED_TRIP_ID, env);
    var firstRoute = pattern.getRoute();

    // apply the update a second time to check that no new route instance is created but the old one is reused
    gtfsRt.applyTripUpdate(tripUpdate);
    var secondPattern = assertAddedTrip(ADDED_TRIP_ID, env);
    var secondRoute = secondPattern.getRoute();

    assertSame(firstRoute, secondRoute);
    assertNotNull(env.transitService().getRoute(firstRoute.getId()));
  }

  @Test
  public void addedTripWithSkippedStop() {
    var tripUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .withTripProperties("A loop", "SW1234")
      .addStopTime(STOP_A_ID, "00:30", DropOffPickupType.PHONE_AGENCY)
      .addSkippedStop(STOP_B_ID, "00:40", DropOffPickupType.COORDINATE_WITH_DRIVER)
      .addSkippedStop(STOP_C_ID, "00:48")
      .addStopTime(STOP_D_ID, "00:55", "A (non-stop)")
      .addStopTime(STOP_A_ID, "01:00")
      .build();

    gtfsRt.applyTripUpdate(tripUpdate);

    // THEN
    final TripPattern tripPattern = assertAddedTrip(ADDED_TRIP_ID, env);
    assertEquals(PickDrop.CALL_AGENCY, tripPattern.getBoardType(0));
    assertEquals(PickDrop.CANCELLED, tripPattern.getAlightType(1));
    assertEquals(PickDrop.CANCELLED, tripPattern.getBoardType(1));
    assertEquals(PickDrop.CANCELLED, tripPattern.getAlightType(2));
    assertEquals(PickDrop.CANCELLED, tripPattern.getBoardType(2));
    assertEquals(PickDrop.SCHEDULED, tripPattern.getAlightType(3));
    var tripTimes = env.tripData(ADDED_TRIP_ID).tripTimes();
    var trip = env.transitService().getTrip(id(ADDED_TRIP_ID));
    assertEquals(I18NString.of("A loop"), Objects.requireNonNull(trip).getHeadsign());
    assertEquals(I18NString.of("A loop"), tripTimes.getHeadsign(0));
    assertFalse(tripTimes.isCancelledStop(0));
    assertTrue(tripTimes.isCancelledStop(1));
    assertTrue(tripTimes.isCancelledStop(2));
    assertFalse(tripTimes.isCancelledStop(3));
    assertEquals(I18NString.of("A (non-stop)"), tripTimes.getHeadsign(3));
  }

  @Test
  public void addedTripWithDelay() {
    var builder = gtfsRt.tripUpdate(ADDED_TRIP_ID, ADDED);

    builder
      .addStopTime(STOP_A_ID, "08:00")
      .addStopTimeWithDelay(STOP_B_ID, "08:35", 300)
      .addStopTimeWithScheduled(STOP_C_ID, "09:10", "09:00");

    var tripUpdate = builder.build();
    gtfsRt.applyTripUpdate(tripUpdate);

    // THEN
    assertAddedTrip(ADDED_TRIP_ID, env);
    var tripTimes = env.tripData(ADDED_TRIP_ID).tripTimes();
    assertEquals(0, tripTimes.getDepartureDelay(0));
    assertEquals(TimeUtils.time("08:00"), tripTimes.getDepartureTime(0));
    assertEquals(300, tripTimes.getArrivalDelay(1));
    assertEquals(TimeUtils.time("08:35"), tripTimes.getArrivalTime(1));
    assertEquals(600, tripTimes.getArrivalDelay(2));
    assertEquals(TimeUtils.time("09:10"), tripTimes.getArrivalTime(2));
  }

  @Test
  void addedTripWithDefaultRoute() {
    var tripUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:40")
      .addStopTime(STOP_C_ID, "00:55")
      .build();

    assertSuccess(gtfsRt.applyTripUpdate(tripUpdate));

    var pattern = assertAddedTrip(ADDED_TRIP_ID, env);
    var route = pattern.getRoute();
    assertEquals(TransitMode.BUS, route.getMode());
    assertEquals(id(ADDED_TRIP_ID), route.getId());
    assertEquals(ADDED_TRIP_ID, route.getLongName().toString());
    assertEquals("autogenerated-gtfs-rt-added-route", route.getAgency().getId().getId());
  }

  @Test
  void addedTripWithExistingRoute() {
    var tripUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .withRouteId("Route1")
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:40")
      .addStopTime(STOP_C_ID, "00:55")
      .build();

    assertSuccess(gtfsRt.applyTripUpdate(tripUpdate));

    var pattern = assertAddedTrip(ADDED_TRIP_ID, env);
    var staticRoute = env.transitService().getRoute(id("Route1"));
    assertSame(staticRoute, pattern.getRoute());
  }

  @Test
  void differentialUpdateOfAddedTrip() {
    var firstUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:40")
      .addStopTime(STOP_C_ID, "00:55")
      .build();

    assertSuccess(gtfsRt.applyTripUpdate(firstUpdate, DIFFERENTIAL));

    var secondUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .addStopTime(STOP_A_ID, "01:00")
      .addStopTime(STOP_B_ID, "01:10")
      .addStopTime(STOP_C_ID, "01:25")
      .build();

    assertSuccess(gtfsRt.applyTripUpdate(secondUpdate, DIFFERENTIAL));

    assertAddedTrip(ADDED_TRIP_ID, env);
    var tripTimes = env.tripData(ADDED_TRIP_ID).tripTimes();
    assertEquals(TimeUtils.time("01:00"), tripTimes.getDepartureTime(0));
    assertEquals(TimeUtils.time("01:10"), tripTimes.getArrivalTime(1));
    assertEquals(TimeUtils.time("01:25"), tripTimes.getArrivalTime(2));
  }

  @Test
  void multipleAddedTripsInSingleBatch() {
    var update1 = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:40")
      .addStopTime(STOP_C_ID, "00:55")
      .build();

    var update2 = gtfsRt
      .tripUpdate("AddedTrip2", ADDED)
      .addStopTime(STOP_A_ID, "01:00")
      .addStopTime(STOP_B_ID, "01:10")
      .addStopTime(STOP_C_ID, "01:25")
      .build();

    var result = gtfsRt.applyTripUpdates(List.of(update1, update2));
    assertEquals(2, result.successful());
    assertAddedTrip(ADDED_TRIP_ID, env);
    assertAddedTrip("AddedTrip2", env);
  }

  private TripPattern assertAddedTrip(String tripId, TransitTestEnvironment env) {
    return assertAddedTrip(tripId, env, RealTimeState.ADDED, STOP_A);
  }

  static TripPattern assertAddedTrip(
    String tripId,
    TransitTestEnvironment env,
    RealTimeState realTimeState,
    RegularStop stop
  ) {
    var tripFetcher = env.tripData(tripId);

    TransitService transitService = env.transitService();
    assertNotNull(tripFetcher.trip());
    assertNotNull(tripFetcher.tripPattern());
    assertNotNull(transitService.getTripOnServiceDate(id(tripId)));

    assertNull(
      tripFetcher.scheduledTripTimes(),
      "Added trip should not be found in scheduled time table"
    );
    assertNotNull(
      tripFetcher.tripTimes(),
      "Added trip should be found in time table for service date"
    );

    assertEquals(realTimeState, tripFetcher.realTimeState());

    // Assert that the tripPattern exists at the given stop
    assertTrue(
      env.timetableSnapshot().getPatternsForStop(stop).contains(tripFetcher.tripPattern())
    );

    return tripFetcher.tripPattern();
  }
}
