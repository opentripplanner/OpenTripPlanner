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

package org.opentripplanner.routing.edgetype;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

public class PartialPlainStreetEdgeTest {
    
    private Graph _graph;
    private StreetVertex v1, v2;
    private PlainStreetEdge e1;
    
    @Before
    public void setUp() throws Exception {
        _graph = new Graph();

        // Graph for a fictional grid city with turn restrictions
        v1 = vertex("maple_1st", 2.0, 2.0);
        v2 = vertex("maple_2nd", 1.0, 2.0);
        
        e1 = edge(v1, v2, 1.0, StreetTraversalPermission.ALL);
    }

    @Test
    public void testConstruction() {
        StreetTraversalPermission perm = StreetTraversalPermission.ALL_DRIVING;
        PartialPlainStreetEdge pEdge = new PartialPlainStreetEdge(e1, v1, v2, e1.getGeometry(),
                "partial e1", e1.getLength(), perm, true);

        assertTrue(pEdge.isEquivalentTo(e1));
        assertTrue(pEdge.isPartial());
        assertTrue(pEdge.isBack());
        assertEquals(e1.getId(), pEdge.getId());
        assertEquals(perm, pEdge.getPermission());
        assertEquals(e1.getCarSpeed(), pEdge.getCarSpeed(), 0.0);

        // Simpler constructor - copies permission from parent edge and sets back to true.
        pEdge = new PartialPlainStreetEdge(e1, v1, v2, e1.getGeometry(), "partial e1",
                e1.getLength());

        assertTrue(pEdge.isEquivalentTo(e1));
        assertTrue(pEdge.isPartial());
        assertFalse(pEdge.isBack());
        assertEquals(e1.getId(), pEdge.getId());
        assertEquals(e1.getPermission(), pEdge.getPermission());
        assertEquals(e1.getCarSpeed(), pEdge.getCarSpeed(), 0.0);
    }
    
    @Test
    public void testTraversal() {
        RoutingRequest options = new RoutingRequest();
        options.setMode(TraverseMode.CAR);
        options.setRoutingContext(_graph, v1, v2);

        // Partial edge with same endpoints as the parent.
        PartialPlainStreetEdge pEdge = new PartialPlainStreetEdge(e1, v1, v2, e1.getGeometry(),
                "partial e1", e1.getLength());
        
        State s0 = new State(options);
        State s1 = e1.traverse(s0);
        
        State partialS0 = new State(options);
        State partialS1 = pEdge.traverse(partialS0);
        
        // Traversal of original and partial edges should yield the same results.
        assertEquals(s1.getTime(), partialS1.getTime());
        assertEquals(s1.getElapsedTime(), partialS1.getElapsedTime());
        assertEquals(s1.getWeight(), partialS1.getWeight(), 0.0);
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
    private PlainStreetEdge edge(StreetVertex vA, StreetVertex vB, double length,
            StreetTraversalPermission perm) {
        String labelA = vA.getLabel();
        String labelB = vB.getLabel();
        String name = String.format("%s_%s", labelA, labelB);
        Coordinate[] coords = new Coordinate[2];
        coords[0] = vA.getCoordinate();
        coords[1] = vB.getCoordinate();
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

        return new PlainStreetEdge(vA, vB, geom, name, length, perm, false, 5.0f);
    }

}
