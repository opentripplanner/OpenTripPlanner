package org.opentripplanner.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.api.common.LocationMatcher;
import org.opentripplanner.model.FeedScopedId;

import java.util.Arrays;
import java.util.List;

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
    public void testWithLabelAndCoord () {
        GenericLocation loc = LocationMatcher.fromOldStyleString("name::1.0,2.5");
        assertEquals("name", loc.label);
        assertNull(loc.placeId);
        assertEquals(Double.valueOf(1.0), loc.lat);
        assertEquals(Double.valueOf(2.5), loc.lng);
        assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());

        loc = LocationMatcher.fromOldStyleString("Label Label::-15.0,  200");
        assertEquals("Label Label", loc.label);
        assertNull(loc.placeId);
        assertEquals(Double.valueOf(-15.0), loc.lat);
        assertEquals(Double.valueOf(200), loc.lng);
        assertEquals(new Coordinate(200, -15), loc.getCoordinate());

        loc = LocationMatcher.fromOldStyleString("A Label::122,-22.3");
        assertEquals("A Label", loc.label);
        assertNull(loc.placeId);
        assertEquals(Double.valueOf(122), loc.lat);
        assertEquals(Double.valueOf(-22.3), loc.lng);
        assertEquals(new Coordinate(-22.3, 122), loc.getCoordinate());
    }

    @Test
    public void testWithId () {
        GenericLocation loc = LocationMatcher.fromOldStyleString("name::aFeed:A1B2C3");
        assertEquals("name", loc.label);
        assertEquals(loc.placeId, new FeedScopedId("aFeed", "A1B2C3"));
        assertNull(loc.lat);
        assertNull(loc.lng);
        assertNull(loc.getCoordinate());

        loc = LocationMatcher.fromOldStyleString("feed:4321");
        assertNull(loc.label);
        assertEquals(loc.placeId, new FeedScopedId("feed", "4321"));
        assertNull(loc.lat);
        assertNull(loc.lng);
        assertNull(loc.getCoordinate());
    }

    @Test
    public void testWithCoordOnly () {
        GenericLocation loc = LocationMatcher.fromOldStyleString("1.0,2.5");
        assertNull(loc.label);
        assertNull(loc.placeId);
        assertEquals(Double.valueOf(1.0), loc.lat);
        assertEquals(Double.valueOf(2.5), loc.lng);
        assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());

        loc = LocationMatcher.fromOldStyleString("    -15.0,  200");
        assertNull(loc.label);
        assertNull(loc.placeId);
        assertEquals(Double.valueOf(-15.0), loc.lat);
        assertEquals(Double.valueOf(200), loc.lng);
        assertEquals(new Coordinate(200, -15), loc.getCoordinate());

        loc = LocationMatcher.fromOldStyleString("122,-22.3   ");
        assertNull(loc.label);
        assertNull(loc.placeId);
        assertEquals(Double.valueOf(122), loc.lat);
        assertEquals(Double.valueOf(-22.3), loc.lng);
        assertEquals(new Coordinate(-22.3, 122), loc.getCoordinate());
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
        assertNull(loc.placeId );

        input = "::";
        loc = LocationMatcher.fromOldStyleString(input);
        assertEquals("", loc.label);
        assertNull(loc.placeId);
    }
}
