package org.opentripplanner.routing.core;

import junit.framework.TestCase;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
