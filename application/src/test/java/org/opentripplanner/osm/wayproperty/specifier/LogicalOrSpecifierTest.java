package org.opentripplanner.osm.wayproperty.specifier;

import org.junit.jupiter.api.Test;

class LogicalOrSpecifierTest extends SpecifierTest {

  OsmSpecifier bikeRoutesSpec = new LogicalOrSpecifier("lcn=yes", "rcn=yes", "ncn=yes");

  @Test
  public void carTunnel() {
    var tunnel = WayTestData.carTunnel();
    assertScore(0, bikeRoutesSpec, tunnel, null);
  }

  @Test
  public void pedestrianTunnel() {
    var tunnel = WayTestData.pedestrianTunnel();
    assertScore(0, bikeRoutesSpec, tunnel, null);
  }

  @Test
  public void wayOnBikeRoute() {
    var way = WayTestData.streetOnBikeRoute();
    assertScore(1, bikeRoutesSpec, way, null);
  }
}
