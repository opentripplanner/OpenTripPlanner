package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.LeftRightEquals;

class OperationTest {

  @Test
  void matches() {
    var op = new LeftRightEquals("cycleway", "lane");
    assertTrue(op.matchesLeft(WayTestData.cyclewayLeft()));
    assertFalse(op.matchesRight(WayTestData.cyclewayLeft()));
  }

  @Test
  void matchesRight() {}
}
