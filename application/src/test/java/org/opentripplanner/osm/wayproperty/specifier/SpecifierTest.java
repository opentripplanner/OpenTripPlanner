package org.opentripplanner.osm.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.opentripplanner.osm.model.OsmWithTags;

public class SpecifierTest {

  protected void assertScore(int expectedScore, OsmSpecifier spec, OsmWithTags tunnel) {
    var result = spec.matchScores(tunnel);
    assertEquals(expectedScore, result.backward());
    assertEquals(expectedScore, result.forward());
    var score = spec.matchScore(tunnel);
    assertEquals(expectedScore, score);
  }
}
