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

package org.opentripplanner.routing.vertextype;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class IntersectionVertexTest {

    private Graph _graph;

    private StreetEdge fromEdge;
    private StreetEdge straightAheadEdge;
    
    @Before
    public void before() {
        _graph = new Graph();

        // Graph for a fictional grid city with turn restrictions
        StreetVertex maple1 = vertex("maple_1st", 2.0, 2.0);
        StreetVertex maple2 = vertex("maple_2nd", 1.0, 2.0);
        StreetVertex maple3 = vertex("maple_3rd", 0.0, 2.0);

        StreetVertex main1 = vertex("main_1st", 2.0, 1.0);
        StreetVertex main2 = vertex("main_2nd", 1.0, 1.0);
        StreetVertex main3 = vertex("main_3rd", 0.0, 1.0);

        StreetVertex broad1 = vertex("broad_1st", 2.0, 0.0);
        StreetVertex broad2 = vertex("broad_2nd", 1.0, 0.0);
        StreetVertex broad3 = vertex("broad_3rd", 0.0, 0.0);

        // Each block along the main streets has unit length and is one-way
        StreetEdge maple1_2 = edge(maple1, maple2, 100.0, false);
        StreetEdge maple2_3 = edge(maple2, maple3, 100.0, false);

        StreetEdge main1_2 = edge(main1, main2, 100.0, false);
        StreetEdge main2_3 = edge(main2, main3, 100.0, false);

        StreetEdge broad1_2 = edge(broad1, broad2, 100.0, false);
        StreetEdge broad2_3 = edge(broad2, broad3, 100.0, false);

        // Each cross-street connects
        StreetEdge maple_main1 = edge(maple1, main1, 50.0, false);
        StreetEdge main_broad1 = edge(main1, broad1, 100.0, false);

        StreetEdge maple_main2 = edge(maple2, main2, 50.0, false);
        StreetEdge main_broad2 = edge(main2, broad2, 50.0, false);

        StreetEdge maple_main3 = edge(maple3, main3, 100.0, false);
        StreetEdge main_broad3 = edge(main3, broad3, 100.0, false);

        this.fromEdge = maple1_2;
        this.straightAheadEdge = maple2_3;
    }

    @Test
    public void testFreeFlowing() {
        IntersectionVertex iv = new IntersectionVertex(_graph, "vertex", 1.0, 2.0);
        assertFalse(iv.freeFlowing);
        
        iv.freeFlowing = (true);
        assertTrue(iv.freeFlowing);
    }
    
    @Test
    public void testInferredFreeFlowing() {
        IntersectionVertex iv = new IntersectionVertex(_graph, "vertex", 1.0, 2.0);
        assertFalse(iv.trafficLight);
        assertFalse(iv.inferredFreeFlowing());
        assertEquals(0, iv.getDegreeIn());
        assertEquals(0, iv.getDegreeOut());
        
        iv.trafficLight = (true);  
        assertTrue(iv.trafficLight);
        assertFalse(iv.inferredFreeFlowing());
        
        iv.addIncoming(fromEdge);
        assertEquals(1, iv.getDegreeIn());
        assertEquals(0, iv.getDegreeOut());
        assertFalse(iv.inferredFreeFlowing());
        
        iv.addOutgoing(straightAheadEdge);
        assertEquals(1, iv.getDegreeIn());
        assertEquals(1, iv.getDegreeOut());
        assertFalse(iv.inferredFreeFlowing());
        
        iv.trafficLight = (false);  
        assertFalse(iv.trafficLight);
        assertTrue(iv.inferredFreeFlowing());
        
        // Set the freeFlowing bit to false.
        iv.freeFlowing = (false);
        assertFalse(iv.freeFlowing);       
    }

    /****
     * Private Methods
     ****/

    private StreetVertex vertex(String label, double lat, double lon) {
        IntersectionVertex v = new IntersectionVertex(_graph, label, lat, lon);
        return v;
    }

    /**
     * Create an edge. If twoWay, create two edges (back and forth).
     * 
     * @param vA
     * @param vB
     * @param length
     * @param back true if this is a reverse edge
     */
    private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length, boolean back) {
        String labelA = vA.getLabel();
        String labelB = vB.getLabel();
        String name = String.format("%s_%s", labelA, labelB);
        Coordinate[] coords = new Coordinate[2];
        coords[0] = vA.getCoordinate();
        coords[1] = vB.getCoordinate();
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

        StreetTraversalPermission perm = StreetTraversalPermission.ALL;
        return new StreetEdge(vA, vB, geom, name, length, perm, back);
    }
}
