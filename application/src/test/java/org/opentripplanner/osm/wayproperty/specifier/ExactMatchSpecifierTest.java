package org.opentripplanner.osm.wayproperty.specifier;

import static org.opentripplanner.osm.model.TraverseDirection.DIRECTIONLESS;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmEntity;

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
    assertScore(200, highwayPrimarySpec, tunnel, DIRECTIONLESS);
    assertScore(0, pedestrianUndergroundIndoorTunnelSpec, tunnel, DIRECTIONLESS);
  }

  @Test
  public void pedestrianTunnelSpecificity() {
    OsmEntity tunnel = WayTestData.pedestrianTunnel();
    assertScore(0, highwayPrimarySpec, tunnel, DIRECTIONLESS);
    assertScore(600, pedestrianUndergroundTunnelSpec, tunnel, DIRECTIONLESS);
    assertScore(800, pedestrianUndergroundIndoorTunnelSpec, tunnel, DIRECTIONLESS);
  }
}
