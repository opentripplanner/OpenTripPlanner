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
import org.opentripplanner.transit.model.site.Entrance;

public class EntranceMapperTest {

  private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "E1");

  private static final String CODE = "Code";

  private static final String DESC = "Desc";

  private static final String DIRECTION = "Direction";

  private static final double LAT = 60.0d;

  private static final double LON = 45.0d;

  private static final String NAME = "Name";

  private static final String PARENT = "Parent";

  private static final String TIMEZONE = "GMT";

  private static final int VEHICLE_TYPE = 5;

  private static final int WHEELCHAIR_BOARDING = 1;

  private static final Accessibility WHEELCHAIR_BOARDING_ENUM = Accessibility.POSSIBLE;

  private static final String ZONE_ID = "Zone Id";

  private static final Stop STOP = new Stop();

  private final EntranceMapper subject = new EntranceMapper(new TranslationHelper(), stationId ->
    null
  );

  static {
    STOP.setLocationType(Stop.LOCATION_TYPE_ENTRANCE_EXIT);
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
  public void testMapCollection() throws Exception {
    assertNull(subject.map((Collection<Stop>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(STOP)).size());
  }

  @Test
  public void testMap() throws Exception {
    Entrance result = subject.map(STOP);

    assertEquals("A:E1", result.getId().toString());
    assertEquals(CODE, result.getCode());
    assertEquals(DESC, result.getDescription().toString());
    assertEquals(LAT, result.getCoordinate().latitude(), 0.0001d);
    assertEquals(LON, result.getCoordinate().longitude(), 0.0001d);
    assertEquals(NAME, result.getName().toString());
    assertEquals(Accessibility.POSSIBLE, result.getWheelchairAccessibility());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    Stop input = new Stop();
    input.setLocationType(Stop.LOCATION_TYPE_ENTRANCE_EXIT);
    input.setId(AGENCY_AND_ID);
    input.setName(NAME);
    input.setLat(LAT);
    input.setLon(LON);

    Entrance result = subject.map(input);

    assertNotNull(result.getId());
    assertNotNull(result.getCoordinate());
    assertNotNull(result.getName());
    assertNull(result.getCode());
    assertNull(result.getDescription());
    assertNull(result.getParentStation());
    assertNull(result.getCode());
    assertEquals(Accessibility.NO_INFORMATION, result.getWheelchairAccessibility());
  }

  @Test
  public void verifyMissingCoordinateThrowsException() {
    Stop input = new Stop();
    input.setLocationType(Stop.LOCATION_TYPE_ENTRANCE_EXIT);
    input.setId(AGENCY_AND_ID);
    input.setName(NAME);

    // Exception expected because the entrence and the parent do not have a coordinate
    assertThrows(IllegalStateException.class, () -> subject.map(input));
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    Entrance result1 = subject.map(STOP);
    Entrance result2 = subject.map(STOP);

    assertSame(result1, result2);
  }
}
