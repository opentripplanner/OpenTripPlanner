package org.opentripplanner.openstreetmap.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class OSMNodeTest {

  @Test
  public void testIsMultiLevel() {
    OSMNode node = new OSMNode();
    assertFalse(node.isMultiLevel());

    node.addTag("highway", "var");
    assertFalse(node.isMultiLevel());

    node.addTag("highway", "elevator");
    assertTrue(node.isMultiLevel());
  }

  @Test
  public void testGetCapacity() {
    OSMNode node = new OSMNode();
    assertFalse(node.hasTag("capacity"));
    assertEquals(0, node.getCapacity());

    try {
      node.addTag("capacity", "foobie");
      node.getCapacity();

      // Above should fail.
      fail();
    } catch (NumberFormatException e) {}

    node.addTag("capacity", "10");
    assertTrue(node.hasTag("capacity"));
    assertEquals(10, node.getCapacity());
  }
}
