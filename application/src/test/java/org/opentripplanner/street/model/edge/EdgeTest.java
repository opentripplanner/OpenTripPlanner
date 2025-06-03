package org.opentripplanner.street.model.edge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model._data.StreetModelForTest.intersectionVertex;

import org.junit.jupiter.api.Test;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.SimpleConcreteEdge;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.Vertex;

public class EdgeTest {

  @Test
  public void testConstruct() {
    Vertex head = StreetModelForTest.intersectionVertex("head", 47.669457, -122.387577);
    Vertex tail = StreetModelForTest.intersectionVertex("tail", 47.669462, -122.384739);
    Edge e = SimpleConcreteEdge.createSimpleConcreteEdge(head, tail);

    assertEquals(head, e.getFromVertex());
    assertEquals(tail, e.getToVertex());
  }

  @Test
  public void testEdgeRemoval() {
    StreetVertex va = intersectionVertex("A", 10.0, 10.0);
    StreetVertex vb = intersectionVertex("B", 10.1, 10.1);
    StreetVertex vc = intersectionVertex("C", 10.2, 10.2);
    StreetVertex vd = intersectionVertex("D", 10.3, 10.3);
    Edge eab = new StreetEdgeBuilder<>()
      .withFromVertex(va)
      .withToVertex(vb)
      .withName("AB")
      .withMeterLength(10)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();
    Edge ebc = new StreetEdgeBuilder<>()
      .withFromVertex(vb)
      .withToVertex(vc)
      .withName("BC")
      .withMeterLength(10)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();
    Edge ecd = new StreetEdgeBuilder<>()
      .withFromVertex(vc)
      .withToVertex(vd)
      .withName("CD")
      .withMeterLength(10)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();
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
