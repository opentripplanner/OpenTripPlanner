package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

class BestMatchSpecifierTest extends SpecifierTest {

  OsmSpecifier highwayPrimary = new BestMatchSpecifier("highway=primary");
  OsmSpecifier pedestrianUndergroundTunnel = new BestMatchSpecifier(
    "highway=footway;layer=-1;tunnel=yes;indoor=yes"
  );

  @Test
  public void carTunnel() {
    var tunnel = WayTestData.carTunnel();
    assertScore(110, highwayPrimary, tunnel);
    assertScore(200, pedestrianUndergroundTunnel, tunnel);
  }

  @Test
  public void pedestrianTunnel() {
    var tunnel = WayTestData.pedestrianTunnel();

    assertScore(0, highwayPrimary, tunnel);
    assertScore(410, pedestrianUndergroundTunnel, tunnel);
  }
}
