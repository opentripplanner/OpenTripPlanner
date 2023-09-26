package org.opentripplanner.openstreetmap.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayProperties;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;

public class FinlandMapperTest {

  static WayPropertySet wps = new WayPropertySet();
  static float epsilon = 0.01f;

  static {
    var source = new FinlandMapper();
    source.populateProperties(wps);
  }

  /**
   * Test that bike and walk safety factors are calculated accurately
   */
  @Test
  public void testSafety() {
    OSMWithTags primaryWay = new OSMWithTags();
    primaryWay.addTag("highway", "primary");
    primaryWay.addTag("oneway", "no");
    OSMWithTags livingStreetWay = new OSMWithTags();
    livingStreetWay.addTag("highway", "living_street");
    OSMWithTags footway = new OSMWithTags();
    footway.addTag("highway", "footway");
    OSMWithTags sidewalk = new OSMWithTags();
    sidewalk.addTag("footway", "sidewalk");
    sidewalk.addTag("highway", "footway");
    OSMWithTags segregatedCycleway = new OSMWithTags();
    segregatedCycleway.addTag("segregated", "yes");
    segregatedCycleway.addTag("highway", "cycleway");
    OSMWithTags tunnel = new OSMWithTags();
    tunnel.addTag("tunnel", "yes");
    tunnel.addTag("highway", "footway");
    OSMWithTags bridge = new OSMWithTags();
    bridge.addTag("bridge", "yes");
    bridge.addTag("highway", "footway");
    OSMWithTags footwayCrossing = new OSMWithTags();
    footwayCrossing.addTag("footway", "crossing");
    footwayCrossing.addTag("highway", "footway");
    OSMWithTags footwayCrossingWithTrafficLights = new OSMWithTags();
    footwayCrossingWithTrafficLights.addTag("footway", "crossing");
    footwayCrossingWithTrafficLights.addTag("highway", "footway");
    footwayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OSMWithTags cyclewayCrossing = new OSMWithTags();
    cyclewayCrossing.addTag("cycleway", "crossing");
    cyclewayCrossing.addTag("highway", "cycleway");
    OSMWithTags cyclewayFootwayCrossing = new OSMWithTags();
    cyclewayFootwayCrossing.addTag("footway", "crossing");
    cyclewayFootwayCrossing.addTag("highway", "cycleway");
    OSMWithTags cyclewayCrossingWithTrafficLights = new OSMWithTags();
    cyclewayCrossingWithTrafficLights.addTag("cycleway", "crossing");
    cyclewayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OSMWithTags cyclewayFootwayCrossingWithTrafficLights = new OSMWithTags();
    cyclewayFootwayCrossingWithTrafficLights.addTag("footway", "crossing");
    cyclewayFootwayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewayFootwayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OSMWithTags cyclewaySegregatedCrossing = new OSMWithTags();
    cyclewaySegregatedCrossing.addTag("cycleway", "crossing");
    cyclewaySegregatedCrossing.addTag("segregated", "yes");
    cyclewaySegregatedCrossing.addTag("highway", "cycleway");
    OSMWithTags cyclewaySegregatedFootwayCrossing = new OSMWithTags();
    cyclewaySegregatedFootwayCrossing.addTag("footway", "crossing");
    cyclewaySegregatedFootwayCrossing.addTag("segregated", "yes");
    cyclewaySegregatedFootwayCrossing.addTag("highway", "cycleway");
    OSMWithTags cyclewaySegregatedCrossingWithTrafficLights = new OSMWithTags();
    cyclewaySegregatedCrossingWithTrafficLights.addTag("cycleway", "crossing");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("segregated", "yes");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OSMWithTags cyclewaySegregatedFootwayCrossingWithTrafficLights = new OSMWithTags();
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
    OSMWithTags wayWithMixins = new OSMWithTags();
    // highway=service has no custom bicycle or walk safety
    wayWithMixins.addTag("highway", "unclassified");
    // surface has mixin bicycle safety of 1.3 but no walk safety
    wayWithMixins.addTag("surface", "metal");
    // 1.0 * 1.3 = 1.3
    assertEquals(1.3, wps.getDataForWay(wayWithMixins).bicycleSafety().forward(), epsilon);
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit > 35 and <= 60 kph
    assertEquals(1.6, wps.getDataForWay(wayWithMixins).walkSafety().forward(), epsilon);

    OSMWithTags wayWithMixinsAndCustomSafety = new OSMWithTags();
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
  }

  @Test
  public void testTagMapping() {
    OSMWithTags way;
    WayProperties wayData;

    way = new OSMWay();
    way.addTag("highway", "unclassified");
    way.addTag("seasonal", "winter");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), NONE);

    way = new OSMWay();
    way.addTag("highway", "trunk");
    way.addTag("ice_road", "yes");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), NONE);

    way = new OSMWay();
    way.addTag("highway", "track");
    way.addTag("winter_road", "yes");
    wayData = wps.getDataForWay(way);
    assertEquals(wayData.getPermission(), NONE);
  }
}
