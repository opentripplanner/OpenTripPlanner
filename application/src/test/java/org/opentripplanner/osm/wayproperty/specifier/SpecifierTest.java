package org.opentripplanner.osm.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.annotation.Nullable;
import org.opentripplanner.osm.TraverseDirection;
import org.opentripplanner.osm.model.OsmEntity;

public class SpecifierTest {

  protected void assertScore(
    int expectedScore,
    OsmSpecifier spec,
    OsmEntity way,
    @Nullable TraverseDirection direction
  ) {
    var score = spec.matchScore(way, direction);
    assertEquals(expectedScore, score);
  }
}
