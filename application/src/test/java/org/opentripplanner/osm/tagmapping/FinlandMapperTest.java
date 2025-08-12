package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

class FinlandMapperTest {

  private WayPropertySet wps;
  private OsmTagMapper mapper;
  static float epsilon = 0.01f;

  @BeforeEach
  void setup() {
    this.wps = new WayPropertySet();
    this.mapper = new FinlandMapper();
    this.mapper.populateProperties(this.wps);
  }

  /**
   * Test that bike and walk safety factors are calculated accurately
   */
  @Test
  void testSafety() {
    var primaryWay = new OsmWay();
    primaryWay.addTag("highway", "primary");
    primaryWay.addTag("oneway", "no");
    var livingStreetWay = new OsmWay();
    livingStreetWay.addTag("highway", "living_street");
    var footway = new OsmWay();
    footway.addTag("highway", "footway");
    var sidewalk = new OsmWay();
    sidewalk.addTag("footway", "sidewalk");
    sidewalk.addTag("highway", "footway");
    var segregatedCycleway = new OsmWay();
    segregatedCycleway.addTag("segregated", "yes");
    segregatedCycleway.addTag("highway", "cycleway");
    var tunnel = new OsmWay();
    tunnel.addTag("tunnel", "yes");
    tunnel.addTag("highway", "footway");
    var bridge = new OsmWay();
    bridge.addTag("bridge", "yes");
    bridge.addTag("highway", "footway");
    var footwayCrossing = new OsmWay();
    footwayCrossing.addTag("footway", "crossing");
    footwayCrossing.addTag("highway", "footway");
    var footwayCrossingWithTrafficLights = new OsmWay();
    footwayCrossingWithTrafficLights.addTag("footway", "crossing");
    footwayCrossingWithTrafficLights.addTag("highway", "footway");
    footwayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    var cyclewayCrossing = new OsmWay();
    cyclewayCrossing.addTag("cycleway", "crossing");
    cyclewayCrossing.addTag("highway", "cycleway");
    var cyclewayFootwayCrossing = new OsmWay();
    cyclewayFootwayCrossing.addTag("footway", "crossing");
    cyclewayFootwayCrossing.addTag("highway", "cycleway");
    var cyclewayCrossingWithTrafficLights = new OsmWay();
    cyclewayCrossingWithTrafficLights.addTag("cycleway", "crossing");
    cyclewayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    var cyclewayFootwayCrossingWithTrafficLights = new OsmWay();
    cyclewayFootwayCrossingWithTrafficLights.addTag("footway", "crossing");
    cyclewayFootwayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewayFootwayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    var cyclewaySegregatedCrossing = new OsmWay();
    cyclewaySegregatedCrossing.addTag("cycleway", "crossing");
    cyclewaySegregatedCrossing.addTag("segregated", "yes");
    cyclewaySegregatedCrossing.addTag("highway", "cycleway");
    var cyclewaySegregatedFootwayCrossing = new OsmWay();
    cyclewaySegregatedFootwayCrossing.addTag("footway", "crossing");
    cyclewaySegregatedFootwayCrossing.addTag("segregated", "yes");
    cyclewaySegregatedFootwayCrossing.addTag("highway", "cycleway");
    var cyclewaySegregatedCrossingWithTrafficLights = new OsmWay();
    cyclewaySegregatedCrossingWithTrafficLights.addTag("cycleway", "crossing");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("segregated", "yes");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    var cyclewaySegregatedFootwayCrossingWithTrafficLights = new OsmWay();
    cyclewaySegregatedFootwayCrossingWithTrafficLights.addTag("footway", "crossing");
    cyclewaySegregatedFootwayCrossingWithTrafficLights.addTag("segregated", "yes");
    cyclewaySegregatedFootwayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewaySegregatedFootwayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    assertEquals(2.06, wps.getDataForWay(primaryWay).forward().bicycleSafety(), epsilon);
    // way with high speed limit, has higher walk safety factor
    assertEquals(1.8, wps.getDataForWay(primaryWay).forward().walkSafety(), epsilon);
    assertEquals(1.8, wps.getDataForWay(primaryWay).backward().walkSafety(), epsilon);
    // way with low speed limit, has lower walk safety factor
    assertEquals(1.45, wps.getDataForWay(livingStreetWay).forward().walkSafety(), epsilon);
    assertEquals(1.1, wps.getDataForWay(footway).forward().walkSafety(), epsilon);
    assertEquals(1.1, wps.getDataForWay(sidewalk).forward().walkSafety(), epsilon);
    assertEquals(1.1, wps.getDataForWay(segregatedCycleway).forward().walkSafety(), epsilon);
    assertEquals(1.0, wps.getDataForWay(tunnel).forward().walkSafety(), epsilon);
    assertEquals(1.0, wps.getDataForWay(bridge).forward().walkSafety(), epsilon);
    assertEquals(1.2, wps.getDataForWay(footwayCrossing).forward().walkSafety(), epsilon);
    assertEquals(
      1.1,
      wps.getDataForWay(footwayCrossingWithTrafficLights).forward().walkSafety(),
      epsilon
    );
    assertEquals(1.25, wps.getDataForWay(cyclewayCrossing).forward().walkSafety(), epsilon);
    assertEquals(1.25, wps.getDataForWay(cyclewayFootwayCrossing).forward().walkSafety(), epsilon);
    assertEquals(
      1.15,
      wps.getDataForWay(cyclewayCrossingWithTrafficLights).forward().walkSafety(),
      epsilon
    );
    assertEquals(
      1.15,
      wps.getDataForWay(cyclewayFootwayCrossingWithTrafficLights).forward().walkSafety(),
      epsilon
    );
    assertEquals(
      1.2,
      wps.getDataForWay(cyclewaySegregatedCrossing).forward().walkSafety(),
      epsilon
    );
    assertEquals(
      1.2,
      wps.getDataForWay(cyclewaySegregatedFootwayCrossing).forward().walkSafety(),
      epsilon
    );
    assertEquals(
      1.1,
      wps.getDataForWay(cyclewaySegregatedCrossingWithTrafficLights).forward().walkSafety(),
      epsilon
    );
    assertEquals(
      1.1,
      wps.getDataForWay(cyclewaySegregatedFootwayCrossingWithTrafficLights).forward().walkSafety(),
      epsilon
    );
  }

  @Test
  void testSafetyWithMixins() {
    var wayWithMixins = new OsmWay();
    // highway=service has no custom bicycle or walk safety
    wayWithMixins.addTag("highway", "unclassified");
    // surface has mixin bicycle safety of 1.3 but no walk safety
    wayWithMixins.addTag("surface", "metal");
    // 1.0 * 1.3 = 1.3
    assertEquals(1.3, wps.getDataForWay(wayWithMixins).forward().bicycleSafety(), epsilon);
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit > 35 and <= 60 kph
    assertEquals(1.6, wps.getDataForWay(wayWithMixins).forward().walkSafety(), epsilon);

    var wayWithMixinsAndCustomSafety = new OsmWay();
    // highway=service has custom bicycle safety of 1.1 but no custom walk safety
    wayWithMixinsAndCustomSafety.addTag("highway", "service");
    // surface has mixin bicycle safety of 1.3 but no walk safety
    wayWithMixinsAndCustomSafety.addTag("surface", "metal");
    // 1.1 * 1.3 = 1.43
    assertEquals(
      1.43,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).forward().bicycleSafety(),
      epsilon
    );
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit <= 35 kph
    assertEquals(
      1.45,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).forward().walkSafety(),
      epsilon
    );

    var wayWithBicycleSidePath = new OsmWay();
    wayWithBicycleSidePath.addTag("bicycle", "use_sidepath");
    assertEquals(8, wps.getDataForWay(wayWithBicycleSidePath).forward().walkSafety(), epsilon);
    var wayWithFootSidePath = new OsmWay();
    wayWithFootSidePath.addTag("foot", "use_sidepath");
    assertEquals(8, wps.getDataForWay(wayWithFootSidePath).forward().walkSafety(), epsilon);
  }

  @Test
  void testTagMapping() {
    WayProperties wayData;

    var way = new OsmWay();
    way.addTag("highway", "unclassified");
    way.addTag("seasonal", "winter");
    wayData = wps.getDataForEntity(way, null);
    assertEquals(wayData.getPermission(), NONE);

    way = new OsmWay();
    way.addTag("highway", "trunk");
    way.addTag("ice_road", "yes");
    wayData = wps.getDataForEntity(way, null);
    assertEquals(wayData.getPermission(), NONE);

    way = new OsmWay();
    way.addTag("highway", "track");
    way.addTag("winter_road", "yes");
    wayData = wps.getDataForEntity(way, null);
    assertEquals(wayData.getPermission(), NONE);
  }

  @Test
  void testWalkingAllowedOnCycleway() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.cycleway(), null).getPermission()
    );
  }

  @Test
  void testCyclingAllowedOnPedestrianAreas() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.pedestrianArea(), null).getPermission()
    );
  }

  /**
   * Test that biking is not allowed in footway areas and transit platforms
   */
  @Test
  void testArea() {
    WayProperties wayData;

    var way = new OsmWay();
    way.addTag("highway", "footway");
    way.addTag("area", "yes");
    wayData = wps.getDataForEntity(way, null);
    assertEquals(PEDESTRIAN, wayData.getPermission());

    way = new OsmWay();
    way.addTag("public_transport", "platform");
    way.addTag("area", "yes");
    wayData = wps.getDataForEntity(way, null);
    assertEquals(PEDESTRIAN, wayData.getPermission());
    way.addTag("bicycle", "yes");
    wayData = wps.getDataForEntity(way, null);
    assertEquals(PEDESTRIAN_AND_BICYCLE, wayData.getPermission());
  }

  @Test
  void serviceNoThroughTraffic() {
    var way = new OsmWay();
    way.addTag("highway", "residential");
    way.addTag("service", "driveway");
    assertTrue(mapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(way));
  }

  @Test
  void motorroad() {
    OsmTagMapper osmTagMapper = new FinlandMapper();
    WayPropertySet wps = new WayPropertySet();
    osmTagMapper.populateProperties(wps);
    var way = WayTestData.carTunnel();
    assertEquals(ALL, wps.getDataForWay(way).forward().getPermission());
    way.addTag("motorroad", "yes");
    assertEquals(CAR, wps.getDataForWay(way).forward().getPermission());
  }
}
