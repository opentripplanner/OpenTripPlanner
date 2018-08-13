package org.opentripplanner.routing.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

public class EdgeTest {
    @Test
    public void testConstruct() {
        Graph graph = new Graph();
        Vertex head = new SimpleConcreteVertex(
                graph, "head", 47.669457, -122.387577);
        Vertex tail = new SimpleConcreteVertex(
                graph, "tail", 47.669462, -122.384739);
        Edge e = new SimpleConcreteEdge(head, tail);

        assertEquals(head, e.getFromVertex());
        assertEquals(tail, e.getToVertex());
        assertTrue(e.getId() >= 0);
    }

    @Test
    public void testEdgeRemoval() {

        Graph graph = new Graph();
        StreetVertex va = new IntersectionVertex(graph, "A", 10.0, 10.0);
        StreetVertex vb = new IntersectionVertex(graph, "B", 10.1, 10.1);
        StreetVertex vc = new IntersectionVertex(graph, "C", 10.2, 10.2);
        StreetVertex vd = new IntersectionVertex(graph, "D", 10.3, 10.3);
        Edge eab = new StreetEdge(va, vb, null, "AB", 10, StreetTraversalPermission.ALL, false);
        Edge ebc = new StreetEdge(vb, vc, null, "BC", 10, StreetTraversalPermission.ALL, false);
        Edge ecd = new StreetEdge(vc, vd, null, "CD", 10, StreetTraversalPermission.ALL, false);

        // remove an edge that is not connected to this vertex
        va.removeOutgoing(ecd);
        assertEquals(va.getDegreeOut(), 1);

        // remove an edge from an edgelist that is empty
        vd.removeOutgoing(eab);
        assertEquals(vd.getDegreeOut(), 0);

        // remove an edge that is actually connected
        assertEquals(va.getDegreeOut(), 1);
        va.removeOutgoing(eab);
        assertEquals(va.getDegreeOut(), 0);

        // remove an edge that is actually connected
        assertEquals(vb.getDegreeIn(), 1);
        assertEquals(vb.getDegreeOut(), 1);
        vb.removeIncoming(eab);
        assertEquals(vb.getDegreeIn(), 0);
        assertEquals(vb.getDegreeOut(), 1);
        vb.removeOutgoing(ebc);
        assertEquals(vb.getDegreeIn(), 0);
        assertEquals(vb.getDegreeOut(), 0);
        
    }
}
