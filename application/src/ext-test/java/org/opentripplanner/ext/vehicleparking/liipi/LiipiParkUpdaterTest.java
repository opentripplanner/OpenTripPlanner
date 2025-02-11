package org.opentripplanner.ext.vehicleparking.liipi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.calendar.openinghours.OpeningHoursCalendarService;
import org.opentripplanner.model.calendar.openinghours.OsmOpeningHoursSupport;
import org.opentripplanner.service.vehicleparking.model.VehicleParkingState;
import org.opentripplanner.test.support.ResourceLoader;
import org.opentripplanner.transit.model.framework.Deduplicator;

public class LiipiParkUpdaterTest {

  private static final ResourceLoader LOADER = ResourceLoader.of(LiipiParkUpdaterTest.class);
  private static final String UTILIZATIONS_URL = LOADER.url("utilizations.json").toString();
  private static final String HUBS_URL = LOADER.url("hubs.json").toString();
  private static final String FACILITIES_URL = LOADER.url("facilities.json").toString();

  @Test
  void parseParks() {
    var timeZone = ZoneIds.HELSINKI;

    var parameters = new LiipiParkUpdaterParameters(
      "",
      3000,
      FACILITIES_URL,
      "liipi",
      null,
      30,
      UTILIZATIONS_URL,
      timeZone,
      HUBS_URL
    );
    var openingHoursCalendarService = new OpeningHoursCalendarService(
      new Deduplicator(),
      LocalDate.of(2022, Month.JANUARY, 1),
      LocalDate.of(2023, Month.JANUARY, 1)
    );
    var updater = new LiipiParkUpdater(parameters, openingHoursCalendarService);

    assertTrue(updater.update());
    var parkingLots = updater.getUpdates();

    assertEquals(4, parkingLots.size());

    var first = parkingLots.get(0);
    assertEquals("Tapiola Park", first.getName().toString());
    assertEquals("liipi:990", first.getId().toString());
    assertEquals(24.804713, first.getCoordinate().longitude());
    assertEquals(60.1760189, first.getCoordinate().latitude());
    var entrance = first.getEntrances().get(0);
    assertEquals(24.804713, entrance.getCoordinate().longitude());
    assertEquals(60.1760189, entrance.getCoordinate().latitude());
    assertTrue(entrance.isCarAccessible());
    assertTrue(entrance.isWalkAccessible());
    assertTrue(first.hasAnyCarPlaces());
    assertFalse(first.hasBicyclePlaces());
    assertFalse(first.hasWheelchairAccessibleCarPlaces());
    assertEquals(1365, first.getCapacity().getCarSpaces());
    assertNull(first.getCapacity().getBicycleSpaces());
    assertNull(first.getCapacity().getWheelchairAccessibleCarSpaces());
    var firstTags = first.getTags();
    assertEquals(7, firstTags.size());
    assertTrue(firstTags.contains("liipi:SERVICE_COVERED"));
    assertTrue(firstTags.contains("liipi:AUTHENTICATION_METHOD_HSL_TICKET"));
    assertTrue(firstTags.contains("liipi:PRICING_METHOD_PAID_10H"));
    assertEquals(VehicleParkingState.OPERATIONAL, first.getState());
    assertTrue(first.hasRealTimeData());
    assertEquals(600, first.getAvailability().getCarSpaces());
    assertNull(first.getAvailability().getBicycleSpaces());
    assertEquals(
      "OHCalendar{" +
      "zoneId: Europe/Helsinki, " +
      "openingHours: [Business days 0:00-23:59:59, " +
      "Saturday 0:00-23:59:59, " +
      "Sunday 0:00-23:59:59]" +
      "}",
      first.getOpeningHours().toString()
    );
    assertEquals(
      "Mo-Fr 0:00-23:59; Sa 0:00-23:59; Su 0:00-23:59",
      OsmOpeningHoursSupport.osmFormat(first.getOpeningHours())
    );

    var firstVehicleParkingGroup = first.getVehicleParkingGroup();
    assertEquals("liipi:321", firstVehicleParkingGroup.id().toString());
    assertEquals("HubYksi", firstVehicleParkingGroup.name().toString(new Locale("fi")));
    assertEquals("HubEn", firstVehicleParkingGroup.name().toString(new Locale("sv")));
    assertEquals(24.804913, firstVehicleParkingGroup.coordinate().longitude());
    assertEquals(60.176064, firstVehicleParkingGroup.coordinate().latitude());

    var second = parkingLots.get(1);
    var name = second.getName();
    assertEquals("Kalasatama (Kauppakeskus REDI)", second.getName().toString());
    assertEquals("Kalasatama (Kauppakeskus REDI)", name.toString(new Locale("fi")));
    assertEquals("Fiskhamnen (Köpcenter REDI)", name.toString(new Locale("sv")));
    assertEquals("Kalasatama (Shopping mall REDI)", name.toString(new Locale("en")));
    assertTrue(second.hasAnyCarPlaces());
    assertFalse(second.hasBicyclePlaces());
    assertTrue(second.hasWheelchairAccessibleCarPlaces());
    assertEquals(300, second.getCapacity().getCarSpaces());
    assertEquals(30, second.getCapacity().getWheelchairAccessibleCarSpaces());
    assertNull(second.getCapacity().getBicycleSpaces());
    assertFalse(second.hasRealTimeData());
    assertNull(second.getAvailability());

    assertEquals(
      "OHCalendar{" +
      "zoneId: Europe/Helsinki, " +
      "openingHours: [Business days 6:00-17:30]" +
      "}",
      second.getOpeningHours().toString()
    );
    assertEquals(firstVehicleParkingGroup, second.getVehicleParkingGroup());

    var third = parkingLots.get(2);
    assertEquals("Alberganpromenadi", third.getName().toString());
    assertFalse(third.hasAnyCarPlaces());
    assertTrue(third.hasBicyclePlaces());
    assertFalse(third.hasWheelchairAccessibleCarPlaces());
    assertNull(third.getCapacity().getCarSpaces());
    assertNull(third.getCapacity().getWheelchairAccessibleCarSpaces());
    assertEquals(80, third.getCapacity().getBicycleSpaces());
    assertTrue(third.hasRealTimeData());
    assertEquals(43, third.getAvailability().getBicycleSpaces());
    assertNull(third.getAvailability().getCarSpaces());
    assertNotEquals(firstVehicleParkingGroup, third.getVehicleParkingGroup());

    var fourth = parkingLots.get(3);
    assertEquals(VehicleParkingState.TEMPORARILY_CLOSED, fourth.getState());
    assertEquals(0, fourth.getTags().size());
    assertEquals(
      "OHCalendar{" +
      "zoneId: Europe/Helsinki, " +
      "openingHours: [Saturday 7:00-18:00, " +
      "Business days 7:00-21:00, " +
      "Sunday 12:00-21:00]" +
      "}",
      fourth.getOpeningHours().toString()
    );
    assertNull(fourth.getVehicleParkingGroup());
  }

  @Test
  void parseParksWithoutTimeZone() {
    ZoneId timeZone = null;

    var parameters = new LiipiParkUpdaterParameters(
      "",
      3000,
      FACILITIES_URL,
      "liipi",
      null,
      30,
      UTILIZATIONS_URL,
      timeZone,
      HUBS_URL
    );
    var openingHoursCalendarService = new OpeningHoursCalendarService(
      new Deduplicator(),
      LocalDate.of(2022, Month.JANUARY, 1),
      LocalDate.of(2023, Month.JANUARY, 1)
    );
    var updater = new LiipiParkUpdater(parameters, openingHoursCalendarService);

    assertTrue(updater.update());
    var parkingLots = updater.getUpdates();

    assertEquals(4, parkingLots.size());

    var first = parkingLots.get(0);
    assertEquals("Tapiola Park", first.getName().toString());
    assertEquals("liipi:990", first.getId().toString());
    assertEquals(24.804713, first.getCoordinate().longitude());
    assertEquals(60.1760189, first.getCoordinate().latitude());
    assertNull(first.getOpeningHours());
  }
}
