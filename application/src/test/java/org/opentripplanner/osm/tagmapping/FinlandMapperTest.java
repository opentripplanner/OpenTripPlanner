package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.OsmWithTags;
import org.opentripplanner.osm.wayproperty.WayProperties;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

public class FinlandMapperTest {

  private WayPropertySet wps;
  private OsmTagMapper mapper;
  static float epsilon = 0.01f;

  @BeforeEach
  public void setup() {
    this.wps = new WayPropertySet();
    this.mapper = new FinlandMapper();
    this.mapper.populateProperties(this.wps);
  }

  /**
   * Test that bike and walk safety factors are calculated accurately
   */
  @Test
  public void testSafety() {
    OsmWithTags primaryWay = new OsmWithTags();
    primaryWay.addTag("highway", "primary");
    primaryWay.addTag("oneway", "no");
    OsmWithTags livingStreetWay = new OsmWithTags();
    livingStreetWay.addTag("highway", "living_street");
    OsmWithTags footway = new OsmWithTags();
    footway.addTag("highway", "footway");
    OsmWithTags sidewalk = new OsmWithTags();
    sidewalk.addTag("footway", "sidewalk");
    sidewalk.addTag("highway", "footway");
    OsmWithTags segregatedCycleway = new OsmWithTags();
    segregatedCycleway.addTag("segregated", "yes");
    segregatedCycleway.addTag("highway", "cycleway");
    OsmWithTags tunnel = new OsmWithTags();
    tunnel.addTag("tunnel", "yes");
    tunnel.addTag("highway", "footway");
    OsmWithTags bridge = new OsmWithTags();
    bridge.addTag("bridge", "yes");
    bridge.addTag("highway", "footway");
    OsmWithTags footwayCrossing = new OsmWithTags();
    footwayCrossing.addTag("footway", "crossing");
    footwayCrossing.addTag("highway", "footway");
    OsmWithTags footwayCrossingWithTrafficLights = new OsmWithTags();
    footwayCrossingWithTrafficLights.addTag("footway", "crossing");
    footwayCrossingWithTrafficLights.addTag("highway", "footway");
    footwayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OsmWithTags cyclewayCrossing = new OsmWithTags();
    cyclewayCrossing.addTag("cycleway", "crossing");
    cyclewayCrossing.addTag("highway", "cycleway");
    OsmWithTags cyclewayFootwayCrossing = new OsmWithTags();
    cyclewayFootwayCrossing.addTag("footway", "crossing");
    cyclewayFootwayCrossing.addTag("highway", "cycleway");
    OsmWithTags cyclewayCrossingWithTrafficLights = new OsmWithTags();
    cyclewayCrossingWithTrafficLights.addTag("cycleway", "crossing");
    cyclewayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OsmWithTags cyclewayFootwayCrossingWithTrafficLights = new OsmWithTags();
    cyclewayFootwayCrossingWithTrafficLights.addTag("footway", "crossing");
    cyclewayFootwayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewayFootwayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OsmWithTags cyclewaySegregatedCrossing = new OsmWithTags();
    cyclewaySegregatedCrossing.addTag("cycleway", "crossing");
    cyclewaySegregatedCrossing.addTag("segregated", "yes");
    cyclewaySegregatedCrossing.addTag("highway", "cycleway");
    OsmWithTags cyclewaySegregatedFootwayCrossing = new OsmWithTags();
    cyclewaySegregatedFootwayCrossing.addTag("footway", "crossing");
    cyclewaySegregatedFootwayCrossing.addTag("segregated", "yes");
    cyclewaySegregatedFootwayCrossing.addTag("highway", "cycleway");
    OsmWithTags cyclewaySegregatedCrossingWithTrafficLights = new OsmWithTags();
    cyclewaySegregatedCrossingWithTrafficLights.addTag("cycleway", "crossing");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("segregated", "yes");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OsmWithTags cyclewaySegregatedFootwayCrossingWithTrafficLights = new OsmWithTags();
    cyclewaySegregatedFootwayCrossingWithTrafficLights.addTag("footway", "crossing");
    cyclewaySegregatedFootwayCrossingWithTrafficLights.addTag("segregated", "yes");
    cyclewaySegregatedFootwayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewaySegregatedFootwayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    assertEquals(2.06, wps.getDataForWay(primaryWay).bicycleSafety().forward(), epsilon);
    // way with high speed limit, has higher walk safety factor
    assertEquals(1.8, wps.getDataForWay(primaryWay).walkSafety().forward(), epsilon);
    assertEquals(1.8, wps.getDataForWay(primaryWay).walkSafety().back(), epsilon);
    // way with low speed limit, has lower walk safety factor
    assertEquals(1.45, wps.getDataForWay(livingStreetWay).walkSafety().forward(), epsilon);
    assertEquals(1.1, wps.getDataForWay(footway).walkSafety().forward(), epsilon);
    assertEquals(1.1, wps.getDataForWay(sidewalk).walkSafety().forward(), epsilon);
    assertEquals(1.1, wps.getDataForWay(segregatedCycleway).walkSafety().forward(), epsilon);
    assertEquals(1.0, wps.getDataForWay(tunnel).walkSafety().forward(), epsilon);
    assertEquals(1.0, wps.getDataForWay(bridge).walkSafety().forward(), epsilon);
    assertEquals(1.2, wps.getDataForWay(footwayCrossing).walkSafety().forward(), epsilon);
    assertEquals(
      1.1,
      wps.getDataForWay(footwayCrossingWithTrafficLights).walkSafety().forward(),
      epsilon
    );
    assertEquals(1.25, wps.getDataForWay(cyclewayCrossing).walkSafety().forward(), epsilon);
    assertEquals(1.25, wps.getDataForWay(cyclewayFootwayCrossing).walkSafety().forward(), epsilon);
    assertEquals(
      1.15,
      wps.getDataForWay(cyclewayCrossingWithTrafficLights).walkSafety().forward(),
      epsilon
    );
    assertEquals(
      1.15,
      wps.getDataForWay(cyclewayFootwayCrossingWithTrafficLights).walkSafety().forward(),
      epsilon
    );
    assertEquals(
      1.2,
      wps.getDataForWay(cyclewaySegregatedCrossing).walkSafety().forward(),
      epsilon
    );
    assertEquals(
      1.2,
      wps.getDataForWay(cyclewaySegregatedFootwayCrossing).walkSafety().forward(),
      epsilon
    );
    assertEquals(
      1.1,
      wps.getDataForWay(cyclewaySegregatedCrossingWithTrafficLights).walkSafety().forward(),
      epsilon
    );
    assertEquals(
      1.1,
      wps.getDataForWay(cyclewaySegregatedFootwayCrossingWithTrafficLights).walkSafety().forward(),
      epsilon
    );
  }

  @Test
  public void testSafetyWithMixins() {
    OsmWithTags wayWithMixins = new OsmWithTags();
    // highway=service has no custom bicycle or walk safety
    wayWithMixins.addTag("highway", "unclassified");
    // surface has mixin bicycle safety of 1.3 but no walk safety
    wayWithMixins.addTag("surface", "metal");
    // 1.0 * 1.3 = 1.3
    assertEquals(1.3, wps.getDataForWay(wayWithMixins).bicycleSafety().forward(), epsilon);
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit > 35 and <= 60 kph
    assertEquals(1.6, wps.getDataForWay(wayWithMixins).walkSafety().forward(), epsilon);

    OsmWithTags wayWithMixinsAndCustomSafety = new OsmWithTags();
    // highway=service has custom bicycle safety of 1.1 but no custom walk safety
    wayWithMixinsAndCustomSafety.addTag("highway", "service");
    // surface has mixin bicycle safety of 1.3 but no walk safety
    wayWithMixinsAndCustomSafety.addTag("surface", "metal");
    // 1.1 * 1.3 = 1.43
    assertEquals(
      1.43,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).bicycleSafety().forward(),
      epsilon
    );
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit <= 35 kph
    assertEquals(
      1.45,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).walkSafety().forward(),
      epsilon
    );

    OsmWithTags wayWithBicycleSidePath = new OsmWithTags();
    wayWithBicycleSidePath.addTag("bicycle", "use_sidepath");
    assertEquals(8, wps.getDataForWay(wayWithBicycleSidePath).walkSafety().forward(), epsilon);
    OsmWithTags wayWithFootSidePath = new OsmWithTags();
    wayWithFootSidePath.addTag("foot", "use_sidepath");
    assertEquals(8, wps.getDataForWay(wayWithFootSidePath).walkSafety().forward(), epsilon);
  }

  @Test
  public void testTagMapping() {
    OsmWithTags way;
    WayProperties wayData;

    way = new OsmWay();
    way.addTag("highway", "unclassified");
    way.addTag("seasonal", "winter");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), NONE);

    way = new OsmWay();
    way.addTag("highway", "trunk");
    way.addTag("ice_road", "yes");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), NONE);

    way = new OsmWay();
    way.addTag("highway", "track");
    way.addTag("winter_road", "yes");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), NONE);
  }

  /**
   * Test that biking is not allowed in footway areas and transit platforms
   */
  @Test
  public void testArea() {
    OsmWithTags way;
    WayProperties wayData;

    way = new OsmWay();
    way.addTag("highway", "footway");
    way.addTag("area", "yes");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), PEDESTRIAN);

    way = new OsmWay();
    way.addTag("public_transport", "platform");
    way.addTag("area", "yes");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), PEDESTRIAN);
    way.addTag("bicycle", "yes");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), PEDESTRIAN_AND_BICYCLE);
  }

  @Test
  public void serviceNoThroughTraffic() {
    var way = new OsmWay();
    way.addTag("highway", "residential");
    way.addTag("service", "driveway");
    assertTrue(mapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(way));
  }
}
