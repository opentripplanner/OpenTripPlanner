package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.service.SiteRepository;

public class StopMapperTest {

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

  private static final String CODE = "Code";

  private static final String DESC = "Desc";

  private static final double LAT = 60.0d;

  private static final double LON = 45.0d;

  private static final String NAME = "Name";

  private static final String PLATFORM_CODE = "Platform Code";

  private static final String TIMEZONE = "GMT";

  private static final String URL = "www.url.me";

  // GTFS vehicle type constants
  private static final int VEHICLE_TYPE_RAIL = 2;
  private static final int VEHICLE_TYPE_BUS = 3;
  private static final int VEHICLE_TYPE_CABLE_CAR = 5;
  private static final int VEHICLE_TYPE_RAIL_REPLACEMENT_BUS = 714;

  private static final int VEHICLE_TYPE = VEHICLE_TYPE_CABLE_CAR;

  private static final int WHEELCHAIR_BOARDING = 1;

  private static final String ZONE_ID = "Zone Id";

  private final StopMapper subject = new StopMapper(
    new IdFactory("A"),
    new TranslationHelper(),
    stationId -> null,
    new SiteRepository().withContext()
  );

  @Test
  void testMapCollection() {
    assertNull(subject.map((Collection<Stop>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(createTestStop())).size());
  }

  @Test
  void testMap() {
    RegularStop result = subject.map(createTestStop());

    assertEquals("A:1", result.getId().toString());
    assertEquals(CODE, result.getCode());
    assertEquals(DESC, result.getDescription().toString());
    assertEquals(LAT, result.getLat(), 0.0001d);
    assertEquals(LON, result.getLon(), 0.0001d);
    assertEquals(NAME, result.getName().toString());
    assertEquals(URL, result.getUrl().toString());
    assertEquals(Accessibility.POSSIBLE, result.getWheelchairAccessibility());
    assertEquals(ZONE_ID, result.getFirstZoneAsString());
    assertFalse(result.isSometimesUsedRealtime());
  }

  @Test
  void testMapWithNulls() {
    Stop input = createMinimalStop();

    RegularStop result = subject.map(input);

    assertNotNull(result.getId());
    assertNull(result.getCode());
    assertNull(result.getDescription());
    assertEquals(NAME, result.getName().toString());
    assertNull(result.getParentStation());
    assertNull(result.getCode());
    assertNull(result.getUrl());
    // Skip getting coordinate, it will throw an exception
    assertEquals(Accessibility.NO_INFORMATION, result.getWheelchairAccessibility());
    assertNull(result.getFirstZoneAsString());
  }

  @Test
  void verifyMissingCoordinateThrowsException() {
    Stop input = createMinimalStop();

    RegularStop result = subject.map(input);

    // Getting the coordinate will throw an IllegalStateException if not set,
    // this is considered to be a implementation error
    var ex = assertThrows(IllegalStateException.class, result::getCoordinate);
    assertEquals("Coordinate not set for: RegularStop{A:1 Name}", ex.getMessage());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  void testMapCache() {
    Stop stop = createTestStop();

    RegularStop result1 = subject.map(stop);
    RegularStop result2 = subject.map(stop);

    assertSame(result1, result2);
  }

  @Test
  void testMapNull() {
    assertNull(subject.map((Stop) null));
  }

  @Test
  void testMapWithInvalidLocationType() {
    Stop input = createBasicStop();
    input.setLocationType(Stop.LOCATION_TYPE_STATION); // Invalid - should be STOP

    var ex = assertThrows(IllegalArgumentException.class, () -> subject.map(input));
    assertEquals(
      "Expected location_type 0, but got 1 for stops.txt entry <Stop A_1>",
      ex.getMessage()
    );
  }

  @Test
  void testMapWithParentStation() {
    TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();
    Station parentStation = testModel.station("Parent").build();

    StopMapper mapperWithStation = new StopMapper(
      new IdFactory("A"),
      new TranslationHelper(),
      id -> parentStation,
      new SiteRepository().withContext()
    );

    Stop input = createBasicStop();
    input.setParentStation("Parent");

    RegularStop result = mapperWithStation.map(input);

    assertNotNull(result.getParentStation());
    assertEquals(parentStation, result.getParentStation());
  }

  @Test
  void testMapWithTimezone() {
    Stop input = createBasicStop();
    input.setTimezone(TIMEZONE);

    RegularStop result = subject.map(input);

    assertNotNull(result.getTimeZone());
    assertEquals(ZoneId.of(TIMEZONE), result.getTimeZone());
  }

  @Test
  void testMapWithVehicleType() {
    Stop input = createBasicStop();
    input.setVehicleType(VEHICLE_TYPE_RAIL);

    RegularStop result = subject.map(input);

    assertNotNull(result.getVehicleType());
    assertEquals(TransitMode.RAIL, result.getVehicleType());
  }

  @Test
  void testMapWithPlatformCode() {
    Stop input = createBasicStop();
    input.setPlatformCode(PLATFORM_CODE);

    RegularStop result = subject.map(input);

    assertEquals(PLATFORM_CODE, result.getPlatformCode());
  }

  @Test
  void testMapSometimesUsedRealtimeForRailWithFeatureOn() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOn(() -> {
      Stop input = createBasicStop();
      input.setVehicleType(VEHICLE_TYPE_RAIL);

      RegularStop result = subject.map(input);

      assertTrue(result.isSometimesUsedRealtime());
    });
  }

  @Test
  void testMapSometimesUsedRealtimeForRailReplacementBusWithFeatureOn() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOn(() -> {
      Stop input = createBasicStop();
      input.setVehicleType(VEHICLE_TYPE_RAIL_REPLACEMENT_BUS);

      RegularStop result = subject.map(input);

      assertTrue(result.isSometimesUsedRealtime());
    });
  }

  @Test
  void testMapSometimesUsedRealtimeForBusWithFeatureOn() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOn(() -> {
      Stop input = createBasicStop();
      input.setVehicleType(VEHICLE_TYPE_BUS);

      RegularStop result = subject.map(input);

      assertFalse(result.isSometimesUsedRealtime());
    });
  }

  @Test
  void testMapSometimesUsedRealtimeWithFeatureOff() {
    OTPFeature.IncludeStopsUsedRealTimeInTransfers.testOff(() -> {
      Stop input = createBasicStop();
      input.setVehicleType(VEHICLE_TYPE_RAIL);

      RegularStop result = subject.map(input);

      assertFalse(result.isSometimesUsedRealtime());
    });
  }

  /**
   * Creates a minimal Stop with only required fields (ID and name).
   */
  private static Stop createMinimalStop() {
    Stop stop = new Stop();
    stop.setId(AGENCY_AND_ID);
    stop.setName(NAME);
    return stop;
  }

  /**
   * Creates a basic Stop with standard fields needed for most tests
   * (ID, name, latitude, longitude, and correct location type).
   */
  private static Stop createBasicStop() {
    Stop stop = new Stop();
    stop.setId(AGENCY_AND_ID);
    stop.setName(NAME);
    stop.setLat(LAT);
    stop.setLon(LON);
    stop.setLocationType(Stop.LOCATION_TYPE_STOP);
    return stop;
  }

  /**
   * Creates a fully populated Stop for comprehensive testing.
   */
  private static Stop createTestStop() {
    Stop stop = new Stop();
    stop.setId(AGENCY_AND_ID);
    stop.setCode(CODE);
    stop.setDesc(DESC);
    stop.setLat(LAT);
    stop.setLon(LON);
    stop.setName(NAME);
    stop.setPlatformCode(PLATFORM_CODE);
    stop.setParentStation("Parent");
    stop.setTimezone(TIMEZONE);
    stop.setUrl(URL);
    stop.setVehicleType(VEHICLE_TYPE);
    stop.setWheelchairBoarding(WHEELCHAIR_BOARDING);
    stop.setZoneId(ZONE_ID);
    stop.setLocationType(Stop.LOCATION_TYPE_STOP);
    return stop;
  }
}
