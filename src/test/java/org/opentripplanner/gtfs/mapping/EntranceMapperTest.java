package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.model.WheelChairBoarding;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EntranceMapperTest  {
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

  private static final WheelChairBoarding WHEELCHAIR_BOARDING_ENUM = WheelChairBoarding.POSSIBLE;

  private static final String ZONE_ID = "Zone Id";

  private static final Stop STOP = new Stop();

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

  private EntranceMapper subject = new EntranceMapper();

  @Test
  public void testMapCollection() throws Exception {
    assertNull(null, subject.map((Collection<Stop>) null));
    assertTrue(subject.map(Collections.emptyList()).isEmpty());
    assertEquals(1, subject.map(Collections.singleton(STOP)).size());
  }

  @Test
  public void testMap() throws Exception {
    org.opentripplanner.model.Entrance result = subject.map(STOP);

    assertEquals("A:E1", result.getId().toString());
    assertEquals(CODE, result.getCode());
    assertEquals(DESC, result.getDescription());
    assertEquals(LAT, result.getLat(), 0.0001d);
    assertEquals(LON, result.getLon(), 0.0001d);
    assertEquals(NAME, result.getName());
    assertEquals(WheelChairBoarding.POSSIBLE, result.getWheelchairBoarding());
  }

  @Test
  public void testMapWithNulls() throws Exception {
    Stop input = new Stop();
    input.setLocationType(Stop.LOCATION_TYPE_ENTRANCE_EXIT);
    input.setId(AGENCY_AND_ID);

    org.opentripplanner.model.Entrance result = subject.map(input);

    assertNotNull(result.getId());
    assertNull(result.getCode());
    assertNull(result.getDescription());
    assertNull(result.getName());
    assertNull(result.getParentStation());
    assertNull(result.getCode());
    assertEquals(WheelChairBoarding.NO_INFORMATION, result.getWheelchairBoarding());
  }

  @Test(expected = IllegalStateException.class)
  public void verifyMissingCoordinateThrowsException() {
    Stop input = new Stop();
    input.setLocationType(Stop.LOCATION_TYPE_ENTRANCE_EXIT);
    input.setId(AGENCY_AND_ID);

    org.opentripplanner.model.Entrance result = subject.map(input);

    // Exception expected because the entrence and the parent do not have a coordinate
    result.getLat();
  }

  /** Mapping the same object twice, should return the the same instance. */
  @Test
  public void testMapCache() throws Exception {
    org.opentripplanner.model.Entrance result1 = subject.map(STOP);
    org.opentripplanner.model.Entrance result2 = subject.map(STOP);

    assertTrue(result1 == result2);
  }
}