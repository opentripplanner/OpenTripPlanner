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
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.site.BoardingArea;
import org.opentripplanner.transit.model.site.RegularStop;

public class BoardingAreaMapperTest {

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "B1");

  private static final String CODE = "Code";

  private static final String DESC = "Desc";

  private static final String DIRECTION = "Direction";

  private static final double LAT = 60.0d;

  private static final double LON = 45.0d;

  private static final String NAME = "Name";

  private static final String PARENT = "ParentStop";

  private static final String TIMEZONE = "GMT";

  private static final int VEHICLE_TYPE = 5;

  private static final int WHEELCHAIR_BOARDING = 1;
  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final RegularStop PARENT_STOP = TEST_MODEL.stop(PARENT).build();

  private static final String ZONE_ID = "Zone Id";
  private static final Stop STOP = new Stop();

  private final BoardingAreaMapper subject = new BoardingAreaMapper(
    new IdFactory("A"),
    new TranslationHelper(),
    stationId -> PARENT_STOP
  );

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
    STOP.setWheelchairBoarding(WHEELCHAIR_BOARDING);
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
    BoardingArea result = subject.map(STOP);

    assertEquals("A:B1", result.getId().toString());
    assertEquals(CODE, result.getCode());
    assertEquals(DESC, result.getDescription().toString());
    assertEquals(LAT, result.getCoordinate().latitude(), 0.0001d);
    assertEquals(LON, result.getCoordinate().longitude(), 0.0001d);
    assertEquals(NAME, result.getName().toString());
    assertEquals(Accessibility.POSSIBLE, result.getWheelchairAccessibility());
    assertEquals(PARENT_STOP, result.getParentStop());
  }

  @Test
  public void testMapWithNulls() {
    Stop input = new Stop();
    input.setLocationType(Stop.LOCATION_TYPE_BOARDING_AREA);
    input.setId(AGENCY_AND_ID);
    input.setParentStation(PARENT);

    BoardingArea result = subject.map(input);

    assertNotNull(result.getId());
    assertNull(result.getCode());
    assertNull(result.getDescription());
    assertEquals(BoardingAreaMapper.DEFAULT_NAME, result.getName().toString());
    assertEquals(PARENT_STOP, result.getParentStop());
    assertNull(result.getCode());
    assertEquals(Accessibility.NO_INFORMATION, result.getWheelchairAccessibility());
  }

  @Test
  public void testThrowsNPEWhenParentUnset() {
    Stop input = new Stop();
    input.setLocationType(Stop.LOCATION_TYPE_BOARDING_AREA);
    input.setId(AGENCY_AND_ID);

    assertThrows(NullPointerException.class, () -> subject.map(input));
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() {
    BoardingArea result1 = subject.map(STOP);
    BoardingArea result2 = subject.map(STOP);

    assertSame(result1, result2);
  }
}
