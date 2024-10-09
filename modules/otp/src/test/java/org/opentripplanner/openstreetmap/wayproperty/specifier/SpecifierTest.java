package org.opentripplanner.openstreetmap.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class SpecifierTest {

  protected void assertScore(int expectedScore, OsmSpecifier spec, OSMWithTags tunnel) {
    var result = spec.matchScores(tunnel);
    assertEquals(expectedScore, result.backward());
    assertEquals(expectedScore, result.forward());
    var score = spec.matchScore(tunnel);
    assertEquals(expectedScore, score);
  }
}
