package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

class BestMatchSpecifierTest {

  OsmSpecifier highwayPrimary = new BestMatchSpecifier("highway=primary");
  OsmSpecifier pedestrianUndergroundTunnel = new BestMatchSpecifier(
    "highway=footway;layer=-1;tunnel=yes;indoor=yes"
  );

  @Test
  public void carTunnel() {
    var tunnel = WayTestData.carTunnel();
    var result = highwayPrimary.matchScores(tunnel);
    assertEquals(110, result.left());

    result = pedestrianUndergroundTunnel.matchScores(tunnel);
    assertEquals(200, result.left());
  }

  @Test
  public void pedestrianTunnelSpecificity() {
    OSMWithTags tunnel = WayTestData.pedestrianTunnel();

    var result = highwayPrimary.matchScores(tunnel);
    assertEquals(0, result.left());

    result = pedestrianUndergroundTunnel.matchScores(tunnel);
    assertEquals(410, result.left());
  }
}
