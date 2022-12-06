package org.opentripplanner.openstreetmap.wayproperty.specifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

class ExactMatchSpecifierTest extends SpecifierTest {

  OsmSpecifier highwayPrimarySpec = new ExactMatchSpecifier("highway=primary");

  OsmSpecifier pedestrianUndergroundTunnelSpec = new ExactMatchSpecifier(
    "highway=footway;layer=-1;tunnel=yes"
  );
  OsmSpecifier pedestrianUndergroundIndoorTunnelSpec = new ExactMatchSpecifier(
    "highway=footway;layer=-1;tunnel=yes;indoor=yes"
  );

  @Test
  public void carTunnel() {
    var tunnel = WayTestData.carTunnel();
    assertScore(200, highwayPrimarySpec, tunnel);
    assertScore(0, pedestrianUndergroundIndoorTunnelSpec, tunnel);
  }

  @Test
  public void pedestrianTunnelSpecificity() {
    OSMWithTags tunnel = WayTestData.pedestrianTunnel();
    assertScore(0, highwayPrimarySpec, tunnel);
    assertScore(600, pedestrianUndergroundTunnelSpec, tunnel);
    assertScore(800, pedestrianUndergroundIndoorTunnelSpec, tunnel);
  }

  @Test
  public void throwOnWildcard() {
    Assertions.assertThrows(
      IllegalArgumentException.class,
      () -> new ExactMatchSpecifier("highway=*")
    );
  }
}
