package org.opentripplanner.osm.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class OsmNodeTest {

  @Test
  public void testIsMultiLevel() {
    OsmNode node = new OsmNode();
    assertFalse(node.isMultiLevel());

    node.addTag("highway", "var");
    assertFalse(node.isMultiLevel());

    node.addTag("highway", "elevator");
    assertTrue(node.isMultiLevel());
  }

  @Test
  public void isBarrier() {
    OsmNode node = new OsmNode();
    assertFalse(node.isBarrier());

    node = new OsmNode();
    node.addTag("barrier", "unknown");
    assertFalse(node.isBarrier());

    node = new OsmNode();
    node.addTag("barrier", "bollard");
    assertTrue(node.isBarrier());

    node = new OsmNode();
    node.addTag("access", "no");
    assertTrue(node.isBarrier());
  }

  public void isTaggedBarrierCrossing() {
    OsmNode node = new OsmNode();
    assertFalse(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("barrier", "gate");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("motor_vehicle", "yes");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("motor_vehicle", "no");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("access", "yes");
    assertTrue(node.isTaggedBarrierCrossing());

    node = new OsmNode();
    node.addTag("access", "no");
    assertTrue(node.isTaggedBarrierCrossing());
  }
}
