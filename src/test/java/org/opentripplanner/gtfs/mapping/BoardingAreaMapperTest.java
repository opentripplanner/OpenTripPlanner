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
import org.opentripplanner.model.WheelchairAccessibility;
import org.opentripplanner.util.TranslationHelper;

public class BoardingAreaMapperTest {

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "B1");

  private static final String CODE = "Code";

  private static final String DESC = "Desc";

  private static final String DIRECTION = "Direction";

  private static final double LAT = 60.0d;

  private static final double LON = 45.0d;

  private static final String NAME = "Name";

  private static final String PARENT = "Parent";

  private static final String TIMEZONE = "GMT";

  private static final int VEHICLE_TYPE = 5;

  private static final WheelchairAccessibility WHEELCHAIR_BOARDING =
    WheelchairAccessibility.POSSIBLE;

  private static final String ZONE_ID = "Zone Id";

  private static final Stop STOP = new Stop();
  private final BoardingAreaMapper subject = new BoardingAreaMapper(new TranslationHelper());

  static {
    STOP.setLocationType(Stop.LOCATION_TYPE_BOARDING_AREA);
    STOP.setId(AGENCY_AND_ID);
    STOP.setCode(CODE);
    STOP.setDesc(DESC);
    STOP.setDirection(DIRECTION);
    STOP.setLat(LAT);
    STOP.setLon(LON);
    STOP.setName(NAME);
    STOP.setParentStation(PARENT);
    STOP.setTimezone(TIMEZONE);
    STOP.setVehicleType(VEHICLE_TYPE);
    STOP.setWheelchairBoarding(WHEELCHAIR_BOARDING.gtfsCode);
    STOP.setZoneId(ZONE_ID);
  }

  @Test
  public void testMapCollection() {
    assertNull(subject.map((Collection<Stop>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(STOP)).size());
  }

  @Test
  public void testMap() {
    org.opentripplanner.model.BoardingArea result = subject.map(STOP);

    assertEquals("A:B1", result.getId().toString());
    assertEquals(CODE, result.getCode());
    assertEquals(DESC, result.getDescription().toString());
    assertEquals(LAT, result.getCoordinate().latitude(), 0.0001d);
    assertEquals(LON, result.getCoordinate().longitude(), 0.0001d);
    assertEquals(NAME, result.getName().toString());
    assertEquals(WHEELCHAIR_BOARDING, result.getWheelchairAccessibility());
  }

  @Test
  public void testMapWithNulls() {
    Stop input = new Stop();
    input.setLocationType(Stop.LOCATION_TYPE_BOARDING_AREA);
    input.setId(AGENCY_AND_ID);

    org.opentripplanner.model.BoardingArea result = subject.map(input);

    assertNotNull(result.getId());
    assertNull(result.getCode());
    assertNull(result.getDescription());
    assertEquals(BoardingAreaMapper.DEFAULT_NAME, result.getName().toString());
    assertNull(result.getParentStop());
    assertNull(result.getCode());
    assertEquals(WheelchairAccessibility.NO_INFORMATION, result.getWheelchairAccessibility());
  }

  @Test
  public void testThrowsNPEWhenCoordinateUnset() {
    Stop input = new Stop();
    input.setLocationType(Stop.LOCATION_TYPE_BOARDING_AREA);
    input.setId(AGENCY_AND_ID);

    org.opentripplanner.model.BoardingArea result = subject.map(input);

    assertThrows(NullPointerException.class, () -> result.getCoordinate().latitude());
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() {
    org.opentripplanner.model.BoardingArea result1 = subject.map(STOP);
    org.opentripplanner.model.BoardingArea result2 = subject.map(STOP);

    assertSame(result1, result2);
  }
}
