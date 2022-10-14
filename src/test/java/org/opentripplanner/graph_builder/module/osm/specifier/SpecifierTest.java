package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class SpecifierTest {

  protected void assertScore(
    int expectedScore,
    OsmSpecifier pedestrianUndergroundTunnelSpec,
    OSMWithTags tunnel
  ) {
    var result = pedestrianUndergroundTunnelSpec.matchScores(tunnel);
    assertEquals(expectedScore, result.left());
    assertEquals(expectedScore, result.right());
    var score = pedestrianUndergroundTunnelSpec.matchScore(tunnel);
    assertEquals(expectedScore, score);
  }
}
