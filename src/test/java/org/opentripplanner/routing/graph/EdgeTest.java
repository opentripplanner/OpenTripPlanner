package org.opentripplanner.routing.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.SplitterVertex;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class EdgeTest {

  @Test
  public void testConstruct() {
    Graph graph = new Graph();
    Vertex head = new SplitterVertex(graph, "head", 47.669457, -122.387577);
    Vertex tail = new SplitterVertex(graph, "tail", 47.669462, -122.384739);
    Edge e = new SimpleConcreteEdge(head, tail);

    assertEquals(head, e.getFromVertex());
    assertEquals(tail, e.getToVertex());
  }

  @Test
  public void testEdgeRemoval() {
    Graph graph = new Graph();
    StreetVertex va = new SplitterVertex(graph, "A", 10.0, 10.0);
    StreetVertex vb = new SplitterVertex(graph, "B", 10.1, 10.1);
    StreetVertex vc = new SplitterVertex(graph, "C", 10.2, 10.2);
    StreetVertex vd = new SplitterVertex(graph, "D", 10.3, 10.3);
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
