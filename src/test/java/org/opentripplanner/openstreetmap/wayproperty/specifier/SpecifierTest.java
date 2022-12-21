package org.opentripplanner.openstreetmap.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class SpecifierTest {

  protected void assertScore(int expectedScore, OsmSpecifier spec, OSMWithTags tunnel) {
    var result = spec.matchScores(tunnel);
    assertEquals(expectedScore, result.left());
    assertEquals(expectedScore, result.right());
    var score = spec.matchScore(tunnel);
    assertEquals(expectedScore, score);
  }
}
