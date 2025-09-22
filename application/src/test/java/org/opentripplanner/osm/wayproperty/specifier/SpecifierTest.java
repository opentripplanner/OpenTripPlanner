package org.opentripplanner.osm.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.TraverseDirection;

public class SpecifierTest {

  protected void assertScore(
    int expectedScore,
    OsmSpecifier spec,
    OsmEntity way,
    TraverseDirection direction
  ) {
    var score = spec.matchScore(way, direction);
    assertEquals(expectedScore, score);
  }
}
