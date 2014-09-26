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

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

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
        FreeEdge ee = new FreeEdge(a, b);
        assertNotNull(ee);
    }

    public void testGetEdgesOneEdge() {
        Graph g = new Graph();
        Vertex a = new IntersectionVertex(g, "A", 5, 5);
        Vertex b = new IntersectionVertex(g, "B", 6, 6);
        FreeEdge ee = new FreeEdge(a, b);

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

    public void testGetStreetEdgesNone() {
        Graph g = new Graph();
        Vertex a = new IntersectionVertex(g, "A", 5, 5);
        Vertex b = new IntersectionVertex(g, "B", 6, 6);
        Vertex c = new IntersectionVertex(g, "C", 3, 2);

        Set<Edge> allEdges = new HashSet<Edge>(4);
        allEdges.add(new FreeEdge(a, b));
        allEdges.add(new FreeEdge(b, c));
        allEdges.add(new FreeEdge(c, b));
        allEdges.add(new FreeEdge(c, a));

        Set<StreetEdge> edges = new HashSet<StreetEdge>(g.getStreetEdges());
        assertEquals(0, edges.size());
    }

    public void testGetStreetEdgesSeveral() {
        Graph g = new Graph();
        StreetVertex a = new IntersectionVertex(g, "A", 5, 5);
        StreetVertex b = new IntersectionVertex(g, "B", 6, 6);
        StreetVertex c = new IntersectionVertex(g, "C", 3, 2);

        Set<Edge> allStreetEdges = new HashSet<Edge>(4);
        allStreetEdges.add(edge(a, b, 1.0));
        allStreetEdges.add(edge(b, c, 1.0));
        allStreetEdges.add(edge(c, b, 1.0));
        allStreetEdges.add(edge(c, a, 1.0));

        Set<StreetEdge> edges = new HashSet<StreetEdge>(g.getStreetEdges());
        assertEquals(4, edges.size());
        assertEquals(allStreetEdges, edges);
    }

    public void testGetEdgesAndVerticesById() {
        Graph g = new Graph();
        StreetVertex a = new IntersectionVertex(g, "A", 5, 5);
        StreetVertex b = new IntersectionVertex(g, "B", 6, 6);
        StreetVertex c = new IntersectionVertex(g, "C", 3, 2);

        Set<Edge> allEdges = new HashSet<Edge>(4);
        allEdges.add(edge(a, b, 1.0));
        allEdges.add(edge(b, c, 1.0));
        allEdges.add(edge(c, b, 1.0));
        allEdges.add(edge(c, a, 1.0));

        // Before rebuilding the indices, they are empty.
        for (Edge e : allEdges) {
            assertNull(g.getEdgeById(e.getId()));
        }

        for (Vertex v : g.getVertices()) {
            assertNull(g.getVertexById(v.getIndex()));
        }

        g.rebuildVertexAndEdgeIndices();
        for (Edge e : allEdges) {
            assertEquals(e, g.getEdgeById(e.getId()));
        }

        for (Vertex v : g.getVertices()) {
            assertEquals(v, g.getVertexById(v.getIndex()));
        }
    }

    /**
     * Create an edge. If twoWay, create two edges (back and forth).
     * 
     * @param vA
     * @param vB
     * @param length
     */
    private StreetEdge edge(StreetVertex vA, StreetVertex vB, double length) {
        String labelA = vA.getLabel();
        String labelB = vB.getLabel();
        String name = String.format("%s_%s", labelA, labelB);
        Coordinate[] coords = new Coordinate[2];
        coords[0] = vA.getCoordinate();
        coords[1] = vB.getCoordinate();
        LineString geom = GeometryUtils.getGeometryFactory().createLineString(coords);

        StreetTraversalPermission perm = StreetTraversalPermission.ALL;
        return new StreetEdge(vA, vB, geom, name, length, perm, false);
    }

}
