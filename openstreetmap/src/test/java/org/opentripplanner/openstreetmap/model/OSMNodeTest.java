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

}
