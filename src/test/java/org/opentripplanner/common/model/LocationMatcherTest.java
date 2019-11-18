package org.opentripplanner.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.api.common.LocationMatcher;

public class LocationMatcherTest {

    @Test
    public void testFromOldStyleString() {
        GenericLocation loc = LocationMatcher.fromOldStyleString("name::12345");
        assertEquals("name", loc.label);
        assertNull(loc.placeId);
        assertNull(loc.lat);
        assertNull(loc.lng);
        assertNull(loc.getCoordinate());
    }
    
    @Test
    public void testFromOldStyleStringWithCoord() {
        GenericLocation loc = LocationMatcher.fromOldStyleString("name::1.0,2.5");
        assertEquals("name", loc.label);
        assertNull(loc.placeId);

        assertEquals(Double.valueOf(1.0), loc.lat);
        assertEquals(Double.valueOf(2.5), loc.lng);
        assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());
    }

    @Test
    public void testFromOldStyleStringIncomplete() {
        String input = "0::";
        GenericLocation loc = LocationMatcher.fromOldStyleString(input);
        assertEquals("0", loc.label);
        assertNull(loc.placeId);

        input = "::1";
        loc = LocationMatcher.fromOldStyleString(input);
        assertEquals("", loc.label);
        assertNull("1", loc.placeId );

        input = "::";
        loc = LocationMatcher.fromOldStyleString(input);
        assertEquals("", loc.label);
        assertNull("", loc.placeId);
    }
}
