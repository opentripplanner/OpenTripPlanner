package org.opentripplanner.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;

public class GenericLocationTest {

    @Test
    public void testEmpty() {
        GenericLocation loc = new GenericLocation();
        assertEquals("", loc.getName());
        assertEquals("", loc.getPlace());
        
        NamedPlace np = loc.getNamedPlace();
        assertEquals("", np.name);
        assertEquals("", np.place);
        
        assertNull(loc.getLat());
        assertNull(loc.getLng());
        assertNull(loc.getCoordinate());
    }

    @Test
    public void testFromNamePlace() {
        GenericLocation loc = new GenericLocation("name", "12345");
        assertEquals("name", loc.getName());
        assertEquals("12345", loc.getPlace());
        
        NamedPlace np = loc.getNamedPlace();
        assertEquals("name", np.name);
        assertEquals("12345", np.place);
        
        assertNull(loc.getLat());
        assertNull(loc.getLng());
        assertNull(loc.getCoordinate());
    }

    @Test
    public void testFromNamePlaceWithCoord() {
        GenericLocation loc = new GenericLocation("name", "1.0,2.5");
        assertEquals("name", loc.getName());
        assertEquals("1.0,2.5", loc.getPlace());

        NamedPlace np = loc.getNamedPlace();
        assertEquals("name", np.name);
        assertEquals("1.0,2.5", np.place);
        
        assertEquals(new Double(1.0), loc.getLat());
        assertEquals(new Double(2.5), loc.getLng());
        assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());
    }

    @Test
    public void testFromOldStyleString() {
        GenericLocation loc = GenericLocation.fromOldStyleString("name::12345");
        assertEquals("name", loc.getName());
        assertEquals("12345", loc.getPlace());
        
        NamedPlace np = loc.getNamedPlace();
        assertEquals("name", np.name);
        assertEquals("12345", np.place);
        
        assertNull(loc.getLat());
        assertNull(loc.getLng());
        assertNull(loc.getCoordinate());
    }
    
    @Test
    public void testFromOldStyleStringWithCoord() {
        GenericLocation loc = GenericLocation.fromOldStyleString("name::1.0,2.5");
        assertEquals("name", loc.getName());
        assertEquals("1.0,2.5", loc.getPlace());
        
        NamedPlace np = loc.getNamedPlace();
        assertEquals("name", np.name);
        assertEquals("1.0,2.5", np.place);
        
        assertEquals(new Double(1.0), loc.getLat());
        assertEquals(new Double(2.5), loc.getLng());
        assertEquals(new Coordinate(2.5, 1.0), loc.getCoordinate());
    }
 
    @Test
    public void testToString() {
        String input = "name::1.0,2.5";
        GenericLocation loc = GenericLocation.fromOldStyleString(input);
        assertEquals(input, loc.toString());
        
        input = "name::12345";
        loc = GenericLocation.fromOldStyleString(input);
        assertEquals(input, loc.toString());
        
        input = "name";
        loc = GenericLocation.fromOldStyleString(input);
        assertEquals(input, loc.toString());
    }
    
}
