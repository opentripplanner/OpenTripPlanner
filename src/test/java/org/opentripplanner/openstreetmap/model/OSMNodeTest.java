/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
