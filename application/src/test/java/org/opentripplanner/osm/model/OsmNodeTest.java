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
}
