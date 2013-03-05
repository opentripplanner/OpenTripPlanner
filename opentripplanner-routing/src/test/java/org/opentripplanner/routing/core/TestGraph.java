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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

public class TestGraph extends TestCase {
    public void testBasic() throws Exception {
        Graph g = new Graph();
        assertNotNull(g);
    }

    public void testAddVertex() throws Exception {
        Graph g = new Graph();
        Vertex a = new IntersectionVertex(g, "A", 5, 5);
        assertEquals(a.getLabel(), "A");
    }

    public void testGetVertex() throws Exception {
        Graph g = new Graph();
        Vertex a = new IntersectionVertex(g, "A", 5, 5);
        Vertex b = g.getVertex("A");
        assertEquals(a, b);
    }

    public void testAddEdge() throws Exception {
        Graph g = new Graph();
        Vertex a = new IntersectionVertex(g, "A", 5, 5);
        Vertex b = new IntersectionVertex(g, "B", 6, 6);
        FreeEdge ee = new FreeEdge(a,b);
        assertNotNull(ee);
    }
    
    public void testGetEdgesOneEdge() {
        Graph g = new Graph();
        Vertex a = new IntersectionVertex(g, "A", 5, 5);
        Vertex b = new IntersectionVertex(g, "B", 6, 6);
        FreeEdge ee = new FreeEdge(a,b);
        
        List<Edge> edges = new ArrayList<Edge>(g.getEdges());
        assertEquals(1, edges.size());
        assertEquals(ee, edges.get(0));
    }
    
    public void testGetEdgesMultiple() {
        Graph g = new Graph();
        Vertex a = new IntersectionVertex(g, "A", 5, 5);
        Vertex b = new IntersectionVertex(g, "B", 6, 6);
        Vertex c = new IntersectionVertex(g, "C", 3, 2);
        
        Set<Edge> expectedEdges = new HashSet<Edge>(4);
        expectedEdges.add(new FreeEdge(a, b));
        expectedEdges.add(new FreeEdge(b, c));
        expectedEdges.add(new FreeEdge(c, b));
        expectedEdges.add(new FreeEdge(c, a));
        
        Set<Edge> edges = new HashSet<Edge>(g.getEdges());
        assertEquals(4, edges.size());
        assertEquals(expectedEdges, edges);
    }
}
