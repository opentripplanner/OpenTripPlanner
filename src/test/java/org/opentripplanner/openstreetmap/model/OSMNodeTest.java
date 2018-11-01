package org.opentripplanner.openstreetmap.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class OSMNodeTest {

    @Test
    public void testIsMultiLevel() {
        OSMNode node = new OSMNode();
        assertFalse(node.isMultiLevel());
        
        node.addTag("highway", "var");
        assertFalse(node.isMultiLevel());
        
        node.addTag("highway", "elevator");
        assertTrue(node.isMultiLevel());
    }
    
    @Test
    public void testGetCapacity() {
        OSMNode node = new OSMNode();
        assertFalse(node.hasTag("capacity"));
        assertEquals(0, node.getCapacity());
        
        try {
            node.addTag("capacity", "foobie");
            node.getCapacity();
            
            // Above should fail.
            assertFalse(true);
        } catch (NumberFormatException e) {}
        
        node.addTag("capacity", "10");
        assertTrue(node.hasTag("capacity"));
        assertEquals(10, node.getCapacity());
    }

}
