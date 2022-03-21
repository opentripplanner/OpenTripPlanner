package org.opentripplanner.routing.core;

import junit.framework.TestCase;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.edgetype.SimpleEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

public class TestOverlayGraph extends TestCase {
    
    public void testBasic() throws Exception {
        Graph g = new Graph();
        Vertex a = new IntersectionVertex(g, "a", 5, 5);
        Vertex b = new IntersectionVertex(g, "b", 6, 5);
        Vertex c = new IntersectionVertex(g, "c", 7, 5);
        Vertex d = new IntersectionVertex(g, "d", 8, 5);
        // vary weights so edges are not considered equal
        Edge ab = new SimpleEdge(a, b, 1, 1);
        Edge bc1 = new SimpleEdge(b, c, 1, 1);
        Edge bc2 = new SimpleEdge(b, c, 2, 2);
        Edge bc3 = new SimpleEdge(b, c, 3, 3);
        Edge cd1 = new SimpleEdge(c, d, 1, 1);
        Edge cd2 = new SimpleEdge(c, d, 2, 2);
        Edge cd3 = new SimpleEdge(c, d, 3, 3);
        OverlayGraph og = new OverlayGraph(g);
        assertEquals(g.countVertices(), og.countVertices());
        assertEquals(g.countEdges(), og.countEdges());
        for (Vertex v : g.getVertices()) {
            for (Edge e : v.getOutgoing()) {
                assertTrue(og.getOutgoing(v).contains(e));
                assertTrue(og.getIncoming(e.getToVertex()).contains(e));
            }
            for (Edge e : v.getIncoming()) {
                assertTrue(og.getIncoming(v).contains(e));
                assertTrue(og.getOutgoing(e.getFromVertex()).contains(e));
            }
        }
      assertEquals(0, og.getIncoming(a).size());
      assertEquals(0, og.getOutgoing(d).size());
        
        // add an edge that is not in the overlay
        Edge ad = new FreeEdge(a, d);
      assertEquals(4, d.getIncoming().size());
      assertEquals(3, og.getIncoming(d).size());
      assertEquals(2, a.getOutgoing().size());
      assertEquals(1, og.getOutgoing(a).size());
        
        // remove edges from overlaygraph
        og.removeEdge(bc1);
        og.removeEdge(bc2);

        assertEquals(og.countEdges(), g.countEdges() - 3);
      assertEquals(1, og.getOutgoing(b).size());
      assertEquals(1, og.getIncoming(c).size());
    }
}
