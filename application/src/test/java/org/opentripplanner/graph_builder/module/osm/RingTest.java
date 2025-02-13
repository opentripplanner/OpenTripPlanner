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
    OsmNode a = new OsmNode();
    a.lat = 0.0;
    a.lon = 0.0;
    OsmNode b = new OsmNode();
    b.lat = 1.0;
    b.lon = 0.0;
    OsmNode c = new OsmNode();
    c.lat = 1.0;
    c.lon = 1.0;
    OsmNode d = new OsmNode();
    d.lat = 0.0;
    d.lon = 1.0;
    OsmNode e = new OsmNode();
    e.lat = 0.5;
    e.lon = 0.5;

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
