package org.opentripplanner.ext.taxizone.graphbuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.core.model.time.LocalDateRange;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.model.FlexStopTimesFactory;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.timetable.Trip;

class TaxiZoneBuilderTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final AreaStop AREA_1 = TEST_MODEL.areaStop("area-1").build();
  private static final AreaStop AREA_2 = TEST_MODEL.areaStop("area-2").build();

  private static final LocalDate SERVICE_DATE = LocalDate.of(2020, 2, 2);

  private static final Trip TRIP = TransitRepositoryForTest.trip("car-pickup")
    .withRoute(TransitRepositoryForTest.route("taxi-route").withMode(TransitMode.TAXI).build())
    .withServiceId(FeedScopedIdForTestFactory.id("service-1"))
    .build();

  private static final CalendarServiceData CALENDAR_SERVICE_DATA = calendarServiceData(
    TRIP.getServiceId(),
    List.of(SERVICE_DATE)
  );

  private static CalendarServiceData calendarServiceData(
    FeedScopedId serviceId,
    List<LocalDate> dates
  ) {
    var calendarServiceData = new CalendarServiceData();
    calendarServiceData.putServiceDatesForServiceId(serviceId, dates);
    return calendarServiceData;
  }

  private static StopTime fullDayAreaStop(
    AreaStop areaStop,
    PickDrop pickupType,
    PickDrop dropOffType,
    Trip trip
  ) {
    return FlexStopTimesFactory.area(areaStop, "00:00", "24:00", trip, pickupType, dropOffType);
  }

  private static StopTime restrictedAreaStop(
    AreaStop areaStop,
    PickDrop pickupType,
    PickDrop dropOffType
  ) {
    return FlexStopTimesFactory.area(areaStop, "10:00", "10:30", TRIP, pickupType, dropOffType);
  }

  private static List<StopTime> validStopTimes() {
    return List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
  }

  private static FlexTrip<?, ?> unscheduledTrip(List<StopTime> stopTimes) {
    return UnscheduledTrip.of(FeedScopedIdForTestFactory.id("t1"))
      .withTrip(TRIP)
      .withStopTimes(stopTimes)
      .build();
  }

  @Test
  void validTripProducesZone() {
    var trip = unscheduledTrip(validStopTimes());
    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertEquals(1, zones.size());
    var zone = zones.get(0);
    assertEquals(AREA_1.getGeometry(), zone.geometry());
    assertEquals(TRIP.getRoute(), zone.route());
    assertEquals(
      LocalDateRange.ofInclusiveEnd(SERVICE_DATE, SERVICE_DATE),
      zone.serviceDateRange()
    );
  }

  @Test
  void notUnscheduledTripIsSkipped() {
    var stopTimes = List.of(
      FlexStopTimesFactory.area(AREA_1, "10:10", "10:15"),
      FlexStopTimesFactory.regularStop("10:40", "10:45")
    );
    var trip = ScheduledDeviatedTrip.of(FeedScopedIdForTestFactory.id("t2"))
      .withStopTimes(stopTimes)
      .build();

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertTrue(zones.isEmpty());
  }

  @Test
  void nonTaxiRouteTypeIsSkipped() {
    var nonTaxiTrip = TransitRepositoryForTest.trip("bus-route")
      .withRoute(TransitRepositoryForTest.route("bus-route").withMode(TransitMode.BUS).build())
      .withServiceId(TRIP.getServiceId())
      .build();
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, nonTaxiTrip),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, nonTaxiTrip)
    );
    var trip = UnscheduledTrip.of(FeedScopedIdForTestFactory.id("t-bus"))
      .withTrip(nonTaxiTrip)
      .withStopTimes(stopTimes)
      .build();

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertTrue(zones.isEmpty());
  }

  @Test
  void missingServiceIdIsSkipped() {
    var noServiceTrip = TransitRepositoryForTest.trip("no-service")
      .withRoute(TransitRepositoryForTest.route("taxi-route-2").withMode(TransitMode.TAXI).build())
      .build();
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, noServiceTrip),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, noServiceTrip)
    );
    var trip = UnscheduledTrip.of(FeedScopedIdForTestFactory.id("t-no-service"))
      .withTrip(noServiceTrip)
      .withStopTimes(stopTimes)
      .build();

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertTrue(zones.isEmpty());
  }

  @Test
  void emptyServiceDatesIsSkipped() {
    var emptyServiceCalendarData = calendarServiceData(TRIP.getServiceId(), List.of());
    var trip = unscheduledTrip(validStopTimes());

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), emptyServiceCalendarData);

    assertTrue(zones.isEmpty());
  }

  @Test
  void nonContiguousServiceDatesIsSkipped() {
    var gappedServiceCalendarData = calendarServiceData(
      TRIP.getServiceId(),
      List.of(SERVICE_DATE, SERVICE_DATE.plusDays(2))
    );
    var trip = unscheduledTrip(validStopTimes());

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), gappedServiceCalendarData);

    assertTrue(zones.isEmpty());
  }

  @Test
  void boundedTimeRestrictionIsSkipped() {
    var stopTimes = List.of(
      restrictedAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE),
      restrictedAreaStop(AREA_1, PickDrop.NONE, PickDrop.COORDINATE_WITH_DRIVER)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertTrue(zones.isEmpty());
  }

  @Test
  void fullDayWindowIsAllowed() {
    var trip = unscheduledTrip(validStopTimes());

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertEquals(1, zones.size());
  }

  @Test
  void wrongNumberOfStopsIsSkipped() {
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertTrue(zones.isEmpty());
  }

  @Test
  void differentAreasIsSkipped() {
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_2, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertTrue(zones.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(value = PickDrop.class, names = { "NONE", "COORDINATE_WITH_DRIVER" })
  void invalidPickupTypeIsSkipped(PickDrop pickupType) {
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, pickupType, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, PickDrop.CALL_AGENCY, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertTrue(zones.isEmpty());
  }

  @ParameterizedTest
  @EnumSource(value = PickDrop.class, names = { "NONE", "COORDINATE_WITH_DRIVER" })
  void invalidDropOffTypeIsSkipped(PickDrop dropOffType) {
    var stopTimes = List.of(
      fullDayAreaStop(AREA_1, PickDrop.CALL_AGENCY, PickDrop.NONE, TRIP),
      fullDayAreaStop(AREA_1, PickDrop.NONE, dropOffType, TRIP)
    );
    var trip = unscheduledTrip(stopTimes);

    var zones = TaxiZoneBuilder.buildZones(List.of(trip), CALENDAR_SERVICE_DATA);

    assertTrue(zones.isEmpty());
  }
}
