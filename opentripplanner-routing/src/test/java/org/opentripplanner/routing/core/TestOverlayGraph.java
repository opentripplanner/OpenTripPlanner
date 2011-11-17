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

package org.opentripplanner.routing.core;

import junit.framework.TestCase;

import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.SimpleEdge;

public class TestOverlayGraph extends TestCase {
    
    public void testBasic() throws Exception {
        Graph g = new Graph();
        Vertex a = new Vertex("a", 5, 5);
        Vertex b = new Vertex("b", 6, 5);
        Vertex c = new Vertex("c", 7, 5);
        Vertex d = new Vertex("d", 8, 5);
        // vary weights so edges are not considered equal
        DirectEdge ab = new SimpleEdge(a, b, 1, 1);
        DirectEdge bc1 = new SimpleEdge(b, c, 1, 1);
        DirectEdge bc2 = new SimpleEdge(b, c, 2, 2);
        DirectEdge bc3 = new SimpleEdge(b, c, 3, 3);
        DirectEdge cd1 = new SimpleEdge(c, d, 1, 1);
        DirectEdge cd2 = new SimpleEdge(c, d, 2, 2);
        DirectEdge cd3 = new SimpleEdge(c, d, 3, 3);
        g.addEdge(ab);
        g.addEdge(bc1);
        g.addEdge(bc2);
        g.addEdge(bc3);
        g.addEdge(cd1);
        g.addEdge(cd2);
        g.addEdge(cd3);
        OverlayGraph og = new OverlayGraph(g);
        assertEquals(g.countVertices(), og.countVertices());
        assertEquals(g.countEdges(), og.countEdges());
        for (Vertex v : g.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                DirectEdge de = (DirectEdge) e;
                assertTrue(og.getOutgoing(v).contains(e));
                assertTrue(og.getIncoming(de.getToVertex()).contains(e));
            }
            for (Edge e : v.getIncoming()) {
                assertTrue(og.getIncoming(v).contains(e));
                assertTrue(og.getOutgoing(e.getFromVertex()).contains(e));
            }
        }
        assertTrue(og.getIncoming(a).size() == 0);
        assertTrue(og.getOutgoing(d).size() == 0);
        
        // add an edge to the overlay that is not in the original
        DirectEdge ad = new FreeEdge(a, d);
        og.addDirectEdge(ad);
        assertTrue(d.getIncoming().size() == 3);
        assertTrue(og.getIncoming(d).size() == 4);
        assertTrue(a.getOutgoing().size() == 1);
        assertTrue(og.getOutgoing(a).size() == 2);
        
        // remove all original edges from overlaygraph
        for (Vertex v : g.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                DirectEdge de = (DirectEdge) e;
                og.removeDirectEdge(de);
            }
        }
        assertEquals(og.countEdges(), 1);
        assertTrue(d.getOutgoing().size() == 0);
        assertTrue(d.getIncoming().size() == 3);
        assertTrue(og.getOutgoing(d).size() == 0);
        assertTrue(og.getIncoming(d).size() == 1);
        assertTrue(a.getIncoming().size() == 0);
        assertTrue(a.getOutgoing().size() == 1);
        assertTrue(og.getOutgoing(a).size() == 1);
        assertTrue(og.getOutgoing(b).size() == 0);
        assertTrue(og.getOutgoing(c).size() == 0);
    }
}
