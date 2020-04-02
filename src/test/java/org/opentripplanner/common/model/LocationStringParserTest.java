package org.opentripplanner.common.model;

import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.api.common.LocationStringParser;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.GenericLocation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LocationStringParserTest {

    @Test
    public void testFromOldStyleString() {
        GenericLocation loc = LocationStringParser.fromOldStyleString("name::12345");
        assertEquals("name", loc.label);
        assertNull(loc.stopId);
        assertNull(loc.lat);
        assertNull(loc.lng);
        assertNull(loc.getCoordinate());
    }
    
    @Test
    public void testWithLabelAndCoord () {
        GenericLocation loc = LocationStringParser.fromOldStyleString("name::1.0,2.5");
        assertEquals("name", loc.label);
        assertNull(loc.stopId);
        assertEquals(Double.valueOf(1.0), loc.lat);
        assertEquals(Double.valueOf(2.5), loc.lng);
        assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());

        loc = LocationStringParser.fromOldStyleString("Label Label::-15.0,  200");
        assertEquals("Label Label", loc.label);
        assertNull(loc.stopId);
        assertEquals(Double.valueOf(-15.0), loc.lat);
        assertEquals(Double.valueOf(200), loc.lng);
        assertEquals(new Coordinate(200, -15), loc.getCoordinate());

        loc = LocationStringParser.fromOldStyleString("A Label::122,-22.3");
        assertEquals("A Label", loc.label);
        assertNull(loc.stopId);
        assertEquals(Double.valueOf(122), loc.lat);
        assertEquals(Double.valueOf(-22.3), loc.lng);
        assertEquals(new Coordinate(-22.3, 122), loc.getCoordinate());
    }

    @Test
    public void testWithId () {
        GenericLocation loc = LocationStringParser.fromOldStyleString("name::aFeed:A1B2C3");
        assertEquals("name", loc.label);
        assertEquals(loc.stopId, new FeedScopedId("aFeed", "A1B2C3"));
        assertNull(loc.lat);
        assertNull(loc.lng);
        assertNull(loc.getCoordinate());

        loc = LocationStringParser.fromOldStyleString("feed:4321");
        assertNull(loc.label);
        assertEquals(loc.stopId, new FeedScopedId("feed", "4321"));
        assertNull(loc.lat);
        assertNull(loc.lng);
        assertNull(loc.getCoordinate());
    }

    @Test
    public void testWithCoordOnly () {
        GenericLocation loc = LocationStringParser.fromOldStyleString("1.0,2.5");
        assertNull(loc.label);
        assertNull(loc.stopId);
        assertEquals(Double.valueOf(1.0), loc.lat);
        assertEquals(Double.valueOf(2.5), loc.lng);
        assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());

        loc = LocationStringParser.fromOldStyleString("    -15.0,  200");
        assertNull(loc.label);
        assertNull(loc.stopId);
        assertEquals(Double.valueOf(-15.0), loc.lat);
        assertEquals(Double.valueOf(200), loc.lng);
        assertEquals(new Coordinate(200, -15), loc.getCoordinate());

        loc = LocationStringParser.fromOldStyleString("122,-22.3   ");
        assertNull(loc.label);
        assertNull(loc.stopId);
        assertEquals(Double.valueOf(122), loc.lat);
        assertEquals(Double.valueOf(-22.3), loc.lng);
        assertEquals(new Coordinate(-22.3, 122), loc.getCoordinate());
    }

    @Test
    public void testFromOldStyleStringIncomplete() {
        String input = "0::";
        GenericLocation loc = LocationStringParser.fromOldStyleString(input);
        assertEquals("0", loc.label);
        assertNull(loc.stopId);

        input = "::1";
        loc = LocationStringParser.fromOldStyleString(input);
        assertEquals("", loc.label);
        assertNull(loc.stopId);

        input = "::";
        loc = LocationStringParser.fromOldStyleString(input);
        assertEquals("", loc.label);
        assertNull(loc.stopId);
    }
}
