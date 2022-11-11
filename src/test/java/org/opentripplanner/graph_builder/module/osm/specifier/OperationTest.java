package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult.EXACT;
import static org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult.NONE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.LeftRightEquals;

class OperationTest {

  @Test
  void matches() {
    var op = new LeftRightEquals("cycleway", "lane");
    assertEquals(EXACT, op.matchLeft(WayTestData.cyclewayLeft()));
    assertEquals(NONE, op.matchRight(WayTestData.cyclewayLeft()));
  }
}
