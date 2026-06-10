package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmNode;

class RingTest {

  @Test
  void testIsNodeConvex() {
    OsmNode a = OsmNode.of().withLatLon(0.0, 0.0).build();
    OsmNode b = OsmNode.of().withLatLon(1.0, 0.0).build();
    OsmNode c = OsmNode.of().withLatLon(1.0, 1.0).build();
    OsmNode d = OsmNode.of().withLatLon(0.0, 1.0).build();
    OsmNode e = OsmNode.of().withLatLon(0.5, 0.5).build();

    Ring ring = new Ring(List.of(a, b, c, d, a));

    assertFalse(ring.isNodeConvex(0));
    assertFalse(ring.isNodeConvex(1));
    assertFalse(ring.isNodeConvex(2));
    assertFalse(ring.isNodeConvex(3));
    assertFalse(ring.isNodeConvex(4));

    ring = new Ring(List.of(a, d, c, b, a));

    assertFalse(ring.isNodeConvex(0));
    assertFalse(ring.isNodeConvex(1));
    assertFalse(ring.isNodeConvex(2));
    assertFalse(ring.isNodeConvex(3));
    assertFalse(ring.isNodeConvex(4));

    ring = new Ring(List.of(a, e, b, c, d, a));

    assertFalse(ring.isNodeConvex(0));
    assertTrue(ring.isNodeConvex(1));
    assertFalse(ring.isNodeConvex(2));
    assertFalse(ring.isNodeConvex(3));
    assertFalse(ring.isNodeConvex(4));
    assertFalse(ring.isNodeConvex(5));

    ring = new Ring(List.of(a, e, d, c, b, a));

    // Ring has been reversed
    assertEquals(0.5, ring.nodes.get(4).lat);

    assertFalse(ring.isNodeConvex(0));
    assertFalse(ring.isNodeConvex(1));
    assertFalse(ring.isNodeConvex(2));
    assertFalse(ring.isNodeConvex(3));
    assertTrue(ring.isNodeConvex(4));
    assertFalse(ring.isNodeConvex(5));
  }
}
