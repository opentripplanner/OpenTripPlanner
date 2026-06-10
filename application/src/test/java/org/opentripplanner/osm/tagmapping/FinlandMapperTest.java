package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

class FinlandMapperTest {

  private final OsmTagMapper mapper = new FinlandMapper();
  private final WayPropertySet wps = mapper.buildWayPropertySet();
  private static final float EPSILON = 0.01f;

  /**
   * Test that bike and walk safety factors are calculated accurately
   */
  @Test
  void testSafety() {
    var primaryWay = OsmWay.of().withTag("highway", "primary").withTag("oneway", "no").build();
    var livingStreetWay = OsmWay.of().withTag("highway", "living_street").build();
    var footway = OsmWay.of().withTag("highway", "footway").build();
    var sidewalk = OsmWay.of().withTag("footway", "sidewalk").withTag("highway", "footway").build();
    var segregatedCycleway = OsmWay.of()
      .withTag("segregated", "yes")
      .withTag("highway", "cycleway")
      .build();
    var tunnel = OsmWay.of().withTag("tunnel", "yes").withTag("highway", "footway").build();
    var bridge = OsmWay.of().withTag("bridge", "yes").withTag("highway", "footway").build();
    var footwayCrossing = OsmWay.of()
      .withTag("footway", "crossing")
      .withTag("highway", "footway")
      .build();
    var footwayCrossingWithTrafficLights = OsmWay.of()
      .withTag("footway", "crossing")
      .withTag("highway", "footway")
      .withTag("crossing", "traffic_signals")
      .build();
    var cyclewayCrossing = OsmWay.of()
      .withTag("cycleway", "crossing")
      .withTag("highway", "cycleway")
      .build();
    var cyclewayFootwayCrossing = OsmWay.of()
      .withTag("footway", "crossing")
      .withTag("highway", "cycleway")
      .build();
    var cyclewayCrossingWithTrafficLights = OsmWay.of()
      .withTag("cycleway", "crossing")
      .withTag("highway", "cycleway")
      .withTag("crossing", "traffic_signals")
      .build();
    var cyclewayFootwayCrossingWithTrafficLights = OsmWay.of()
      .withTag("footway", "crossing")
      .withTag("highway", "cycleway")
      .withTag("crossing", "traffic_signals")
      .build();
    var cyclewaySegregatedCrossing = OsmWay.of()
      .withTag("cycleway", "crossing")
      .withTag("segregated", "yes")
      .withTag("highway", "cycleway")
      .build();
    var cyclewaySegregatedFootwayCrossing = OsmWay.of()
      .withTag("footway", "crossing")
      .withTag("segregated", "yes")
      .withTag("highway", "cycleway")
      .build();
    var cyclewaySegregatedCrossingWithTrafficLights = OsmWay.of()
      .withTag("cycleway", "crossing")
      .withTag("segregated", "yes")
      .withTag("highway", "cycleway")
      .withTag("crossing", "traffic_signals")
      .build();
    var cyclewaySegregatedFootwayCrossingWithTrafficLights = OsmWay.of()
      .withTag("footway", "crossing")
      .withTag("segregated", "yes")
      .withTag("highway", "cycleway")
      .withTag("crossing", "traffic_signals")
      .build();
    assertEquals(2.06, wps.getDataForWay(primaryWay).forward().bicycleSafety(), EPSILON);
    // way with high speed limit, has higher walk safety factor
    assertEquals(1.8, wps.getDataForWay(primaryWay).forward().walkSafety(), EPSILON);
    assertEquals(1.8, wps.getDataForWay(primaryWay).backward().walkSafety(), EPSILON);
    // way with low speed limit, has lower walk safety factor
    assertEquals(1.45, wps.getDataForWay(livingStreetWay).forward().walkSafety(), EPSILON);
    assertEquals(1.1, wps.getDataForWay(footway).forward().walkSafety(), EPSILON);
    assertEquals(1.1, wps.getDataForWay(sidewalk).forward().walkSafety(), EPSILON);
    assertEquals(1.1, wps.getDataForWay(segregatedCycleway).forward().walkSafety(), EPSILON);
    assertEquals(1.0, wps.getDataForWay(tunnel).forward().walkSafety(), EPSILON);
    assertEquals(1.0, wps.getDataForWay(bridge).forward().walkSafety(), EPSILON);
    assertEquals(1.2, wps.getDataForWay(footwayCrossing).forward().walkSafety(), EPSILON);
    assertEquals(
      1.1,
      wps.getDataForWay(footwayCrossingWithTrafficLights).forward().walkSafety(),
      EPSILON
    );
    assertEquals(1.25, wps.getDataForWay(cyclewayCrossing).forward().walkSafety(), EPSILON);
    assertEquals(1.25, wps.getDataForWay(cyclewayFootwayCrossing).forward().walkSafety(), EPSILON);
    assertEquals(
      1.15,
      wps.getDataForWay(cyclewayCrossingWithTrafficLights).forward().walkSafety(),
      EPSILON
    );
    assertEquals(
      1.15,
      wps.getDataForWay(cyclewayFootwayCrossingWithTrafficLights).forward().walkSafety(),
      EPSILON
    );
    assertEquals(
      1.2,
      wps.getDataForWay(cyclewaySegregatedCrossing).forward().walkSafety(),
      EPSILON
    );
    assertEquals(
      1.2,
      wps.getDataForWay(cyclewaySegregatedFootwayCrossing).forward().walkSafety(),
      EPSILON
    );
    assertEquals(
      1.1,
      wps.getDataForWay(cyclewaySegregatedCrossingWithTrafficLights).forward().walkSafety(),
      EPSILON
    );
    assertEquals(
      1.1,
      wps.getDataForWay(cyclewaySegregatedFootwayCrossingWithTrafficLights).forward().walkSafety(),
      EPSILON
    );
  }

  @Test
  void testSafetyWithMixins() {
    var wayWithMixins = OsmWay.of()
      // highway=service has no custom bicycle or walk safety
      .withTag("highway", "unclassified")
      // surface has mixin bicycle safety of 1.3 but no walk safety
      .withTag("surface", "metal")
      .build();
    // 1.0 * 1.3 = 1.3
    assertEquals(1.3, wps.getDataForWay(wayWithMixins).forward().bicycleSafety(), EPSILON);
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit > 35 and <= 60 kph
    assertEquals(1.6, wps.getDataForWay(wayWithMixins).forward().walkSafety(), EPSILON);

    var wayWithMixinsAndCustomSafety = OsmWay.of()
      // highway=service has custom bicycle safety of 1.1 but no custom walk safety
      .withTag("highway", "service")
      // surface has mixin bicycle safety of 1.3 but no walk safety
      .withTag("surface", "metal")
      .build();
    // 1.1 * 1.3 = 1.43
    assertEquals(
      1.43,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).forward().bicycleSafety(),
      EPSILON
    );
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit <= 35 kph
    assertEquals(
      1.45,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).forward().walkSafety(),
      EPSILON
    );
  }

  @Test
  void testUseSidePath() {
    var wayWithBicycleSidePath = OsmWay.of().withTag("bicycle", "use_sidepath").build();
    assertEquals(9, wps.getDataForWay(wayWithBicycleSidePath).forward().walkSafety(), EPSILON);
    var wayWithFootSidePath = OsmWay.of().withTag("foot", "use_sidepath").build();
    assertEquals(9, wps.getDataForWay(wayWithFootSidePath).forward().walkSafety(), EPSILON);
    var wayWithBoth = OsmWay.of()
      .withTag("foot", "use_sidepath")
      .withTag("bicycle", "use_sidepath")
      .build();
    assertEquals(9, wps.getDataForWay(wayWithBoth).forward().walkSafety(), EPSILON);
  }

  @Test
  void testMaxCarSpeed() {
    assertEquals(33.34, wps.maxPossibleCarSpeed(), EPSILON);
    assertEquals(22.21, wps.defaultCarSpeed(), EPSILON);
  }

  @Test
  void testTagMapping() {
    WayProperties wayData;

    var way = OsmWay.of().withTag("highway", "unclassified").withTag("seasonal", "winter").build();
    wayData = wps.getDataForEntity(way);
    assertEquals(wayData.getPermission(), NONE);

    way = OsmWay.of().withTag("highway", "trunk").withTag("ice_road", "yes").build();
    wayData = wps.getDataForEntity(way);
    assertEquals(wayData.getPermission(), NONE);

    way = OsmWay.of().withTag("highway", "track").withTag("winter_road", "yes").build();
    wayData = wps.getDataForEntity(way);
    assertEquals(wayData.getPermission(), NONE);
  }

  @Test
  void testWalkingAllowedOnCycleway() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.cycleway()).getPermission()
    );
  }

  @Test
  void testCyclingAllowedOnPedestrianAreas() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.pedestrianArea()).getPermission()
    );
  }

  /**
   * Test that biking is not allowed in footway areas and transit platforms
   */
  @Test
  void testArea() {
    WayProperties wayData;

    var way = OsmWay.of().withTag("highway", "footway").withTag("area", "yes").build();
    wayData = wps.getDataForEntity(way);
    assertEquals(PEDESTRIAN, wayData.getPermission());

    way = OsmWay.of().withTag("public_transport", "platform").withTag("area", "yes").build();
    wayData = wps.getDataForEntity(way);
    assertEquals(PEDESTRIAN, wayData.getPermission());
    way = way.copy().withTag("bicycle", "yes").build();
    wayData = wps.getDataForEntity(way);
    assertEquals(PEDESTRIAN_AND_BICYCLE, wayData.getPermission());
  }

  @Test
  void serviceNoThroughTraffic() {
    var way = OsmWay.of().withTag("highway", "residential").withTag("service", "driveway").build();
    assertTrue(mapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(way));
  }

  @Test
  void motorroad() {
    var way = WayTestData.carTunnel();
    assertEquals(ALL, wps.getDataForWay(way).forward().getPermission());
    way = way.copy().withTag("motorroad", "yes").build();
    assertEquals(CAR, wps.getDataForWay(way).forward().getPermission());
  }
}
