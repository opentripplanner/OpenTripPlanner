package org.opentripplanner.osm.wayproperty.specifier;

import static org.opentripplanner.osm.model.TraverseDirection.DIRECTIONLESS;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.TraverseDirection;

class LogicalOrSpecifierTest extends SpecifierTest {

  OsmSpecifier bikeRoutesSpec = new LogicalOrSpecifier("lcn=yes", "rcn=yes", "ncn=yes");

  @Test
  public void carTunnel() {
    var tunnel = WayTestData.carTunnel();
    assertScore(0, bikeRoutesSpec, tunnel, DIRECTIONLESS);
  }

  @Test
  public void pedestrianTunnel() {
    var tunnel = WayTestData.pedestrianTunnel();
    assertScore(0, bikeRoutesSpec, tunnel, DIRECTIONLESS);
  }

  @Test
  public void wayOnBikeRoute() {
    var way = WayTestData.streetOnBikeRoute();
    assertScore(1, bikeRoutesSpec, way, DIRECTIONLESS);
  }
}
