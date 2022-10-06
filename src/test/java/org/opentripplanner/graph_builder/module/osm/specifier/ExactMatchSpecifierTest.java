package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

class ExactMatchSpecifierTest {

  OsmSpecifier highwayPrimary = new ExactMatchSpecifier("highway=primary");
  OsmSpecifier pedestrianUndergroundTunnel = new ExactMatchSpecifier(
    "highway=footway;layer=-1;tunnel=yes;indoor=yes"
  );

  @Test
  public void carTunnel() {
    var tunnel = WayTestData.carTunnel();
    var result = highwayPrimary.matchScores(tunnel);
    assertEquals(Integer.MAX_VALUE, result.left());

    result = pedestrianUndergroundTunnel.matchScores(tunnel);
    assertEquals(Integer.MIN_VALUE, result.left());
  }

  @Test
  public void pedestrianTunnelSpecificity() {
    OSMWithTags tunnel = WayTestData.pedestrianTunnel();

    var result = highwayPrimary.matchScores(tunnel);
    assertEquals(Integer.MIN_VALUE, result.left());

    result = pedestrianUndergroundTunnel.matchScores(tunnel);
    assertEquals(Integer.MAX_VALUE, result.left());
  }
}
