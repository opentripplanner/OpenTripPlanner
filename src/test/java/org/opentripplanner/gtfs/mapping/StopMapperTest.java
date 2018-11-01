package org.opentripplanner.gtfs.mapping;

import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class StopMapperTest {
    private static final AgencyAndId AGENCY_AND_ID = new AgencyAndId("A", "1");

    private static final String CODE = "Code";

    private static final String DESC = "Desc";

    private static final String DIRECTION = "Direction";

    private static final double LAT = 60.0d;

    private static final double LON = 45.0d;

    private static final int LOCATION_TYPE = 1;

    private static final String NAME = "Name";

    private static final String PARENT = "Parent";

    private static final String PLATFORM_CODE = "Platform Code";

    private static final String TIMEZONE = "GMT";

    private static final String URL = "www.url.me";

    private static final int VEHICLE_TYPE = 5;

    private static final int WHEELCHAIR_BOARDING = 1;

    private static final String ZONE_ID = "Zone Id";

    private static final Stop STOP = new Stop();

    static {
        STOP.setId(AGENCY_AND_ID);
        STOP.setCode(CODE);
        STOP.setDesc(DESC);
        STOP.setDirection(DIRECTION);
        STOP.setLat(LAT);
        STOP.setLon(LON);
        STOP.setLocationType(LOCATION_TYPE);
        STOP.setName(NAME);
        STOP.setParentStation(PARENT);
        STOP.setPlatformCode(PLATFORM_CODE);
        STOP.setTimezone(TIMEZONE);
        STOP.setUrl(URL);
        STOP.setVehicleType(VEHICLE_TYPE);
        STOP.setWheelchairBoarding(WHEELCHAIR_BOARDING);
        STOP.setZoneId(ZONE_ID);
    }

    private StopMapper subject = new StopMapper();

    @Test
    public void testMapCollection() throws Exception {
        assertNull(null, subject.map((Collection<Stop>) null));
        assertTrue(subject.map(Collections.emptyList()).isEmpty());
        assertEquals(1, subject.map(Collections.singleton(STOP)).size());
    }

    @Test
    public void testMap() throws Exception {
        org.opentripplanner.model.Stop result = subject.map(STOP);

        assertEquals("A_1", result.getId().toString());
        assertEquals(CODE, result.getCode());
        assertEquals(DESC, result.getDesc());
        assertEquals(DIRECTION, result.getDirection());
        assertEquals(LAT, result.getLat(), 0.0001d);
        assertEquals(LON, result.getLon(), 0.0001d);
        assertEquals(LOCATION_TYPE, result.getLocationType());
        assertEquals(NAME, result.getName());
        assertEquals(PARENT, result.getParentStation());
        assertEquals(PLATFORM_CODE, result.getPlatformCode());
        assertEquals(TIMEZONE, result.getTimezone());
        assertEquals(URL, result.getUrl());
        assertEquals(VEHICLE_TYPE, result.getVehicleType());
        assertEquals(WHEELCHAIR_BOARDING, result.getWheelchairBoarding());
        assertEquals(ZONE_ID, result.getZoneId());
    }

    @Test
    public void testMapWithNulls() throws Exception {
        Stop input = new Stop();
        input.setId(AGENCY_AND_ID);

        org.opentripplanner.model.Stop result = subject.map(input);

        assertNotNull(result.getId());
        assertNull(result.getCode());
        assertNull(result.getDesc());
        assertNull(result.getDirection());
        assertEquals(0d, result.getLat(), 0.0001);
        assertEquals(0d, result.getLon(), 0.0001);
        assertEquals(0, result.getLocationType());
        assertNull(result.getName());
        assertNull(result.getParentStation());
        assertNull(result.getPlatformCode());
        assertNull(result.getTimezone());
        assertNull(result.getUrl());
        assertFalse(result.isVehicleTypeSet());
        assertEquals(0, result.getWheelchairBoarding());
        assertNull(result.getZoneId());
    }

    /** Mapping the same object twice, should return the the same instance. */
    @Test
    public void testMapCache() throws Exception {
        org.opentripplanner.model.Stop result1 = subject.map(STOP);
        org.opentripplanner.model.Stop result2 = subject.map(STOP);

        assertTrue(result1 == result2);
    }
}