package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.SiteRepository;

public class StopAndStationMapperTest {

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

  private static final String CODE = "Code";

  private static final String DESC = "Desc";

  private static final String DIRECTION = "Direction";

  private static final double LAT = 60.0d;

  private static final double LON = 45.0d;

  private static final String NAME = "Name";

  private static final String PARENT = "Parent";

  private static final String PLATFORM_CODE = "Platform Code";

  private static final String TIMEZONE = "GMT";

  private static final String URL = "www.url.me";

  private static final int VEHICLE_TYPE = 5;

  private static final int WHEELCHAIR_BOARDING = 1;

  private static final String ZONE_ID = "Zone Id";

  private static final Stop STOP = new Stop();

  private final StopMapper subject = new StopMapper(
    new IdFactory("A"),
    new TranslationHelper(),
    stationId -> null,
    new SiteRepository().withContext()
  );

  static {
    STOP.setId(AGENCY_AND_ID);
    STOP.setCode(CODE);
    STOP.setDesc(DESC);
    STOP.setDirection(DIRECTION);
    STOP.setLat(LAT);
    STOP.setLon(LON);
    STOP.setName(NAME);
    STOP.setParentStation(PARENT);
    STOP.setPlatformCode(PLATFORM_CODE);
    STOP.setTimezone(TIMEZONE);
    STOP.setUrl(URL);
    STOP.setVehicleType(VEHICLE_TYPE);
    STOP.setWheelchairBoarding(WHEELCHAIR_BOARDING);
    STOP.setZoneId(ZONE_ID);
  }

  @Test
  void testMapCollection() {
    assertNull(subject.map((Collection<Stop>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(STOP)).size());
  }

  @Test
  void testMap() {
    RegularStop result = subject.map(STOP);

    assertEquals("A:1", result.getId().toString());
    assertEquals(CODE, result.getCode());
    assertEquals(DESC, result.getDescription().toString());
    assertEquals(LAT, result.getLat(), 0.0001d);
    assertEquals(LON, result.getLon(), 0.0001d);
    assertEquals(NAME, result.getName().toString());
    assertEquals(URL, result.getUrl().toString());
    assertEquals(Accessibility.POSSIBLE, result.getWheelchairAccessibility());
    assertEquals(ZONE_ID, result.getFirstZoneAsString());
  }

  @Test
  void testMapWithNulls() {
    Stop input = new Stop();
    input.setId(AGENCY_AND_ID);
    input.setName(NAME);

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
    Stop input = new Stop();
    input.setId(AGENCY_AND_ID);
    input.setName(NAME);

    RegularStop result = subject.map(input);

    // Getting the coordinate will throw an IllegalArgumentException if not set,
    // this is considered to be a implementation error
    assertThrows(IllegalStateException.class, result::getCoordinate);
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  void testMapCache() {
    RegularStop result1 = subject.map(STOP);
    RegularStop result2 = subject.map(STOP);

    assertSame(result1, result2);
  }
}
