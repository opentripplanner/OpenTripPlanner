package org.opentripplanner.graph_builder.module.osm.tagmapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.tagmapping.FinlandMapper;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

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
    OSMWithTags cyclewayCrossingWithTrafficLights = new OSMWithTags();
    cyclewayCrossingWithTrafficLights.addTag("cycleway", "crossing");
    cyclewayCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewayCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    OSMWithTags cyclewaySegregatedCrossing = new OSMWithTags();
    cyclewaySegregatedCrossing.addTag("cycleway", "crossing");
    cyclewaySegregatedCrossing.addTag("segregated", "yes");
    cyclewaySegregatedCrossing.addTag("highway", "cycleway");
    OSMWithTags cyclewaySegregatedCrossingWithTrafficLights = new OSMWithTags();
    cyclewaySegregatedCrossingWithTrafficLights.addTag("cycleway", "crossing");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("segregated", "yes");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("highway", "cycleway");
    cyclewaySegregatedCrossingWithTrafficLights.addTag("crossing", "traffic_signals");
    assertEquals(2.06, wps.getDataForWay(primaryWay).getBicycleSafetyFeatures().forward(), epsilon);
    // way with high speed limit, has higher walk safety factor
    assertEquals(1.8, wps.getDataForWay(primaryWay).getWalkSafetyFeatures().forward(), epsilon);
    assertEquals(1.8, wps.getDataForWay(primaryWay).getWalkSafetyFeatures().back(), epsilon);
    // way with low speed limit, has lower walk safety factor
    assertEquals(
      1.45,
      wps.getDataForWay(livingStreetWay).getWalkSafetyFeatures().forward(),
      epsilon
    );
    assertEquals(1.1, wps.getDataForWay(footway).getWalkSafetyFeatures().forward(), epsilon);
    assertEquals(1.1, wps.getDataForWay(sidewalk).getWalkSafetyFeatures().forward(), epsilon);
    assertEquals(
      1.1,
      wps.getDataForWay(segregatedCycleway).getWalkSafetyFeatures().forward(),
      epsilon
    );
    assertEquals(1.0, wps.getDataForWay(tunnel).getWalkSafetyFeatures().forward(), epsilon);
    assertEquals(1.0, wps.getDataForWay(bridge).getWalkSafetyFeatures().forward(), epsilon);
    assertEquals(
      1.3,
      wps.getDataForWay(footwayCrossing).getWalkSafetyFeatures().forward(),
      epsilon
    );
    assertEquals(
      1.2,
      wps.getDataForWay(footwayCrossingWithTrafficLights).getWalkSafetyFeatures().forward(),
      epsilon
    );
    assertEquals(
      1.35,
      wps.getDataForWay(cyclewayCrossing).getWalkSafetyFeatures().forward(),
      epsilon
    );
    assertEquals(
      1.25,
      wps.getDataForWay(cyclewayCrossingWithTrafficLights).getWalkSafetyFeatures().forward(),
      epsilon
    );
    assertEquals(
      1.3,
      wps.getDataForWay(cyclewaySegregatedCrossing).getWalkSafetyFeatures().forward(),
      epsilon
    );
    assertEquals(
      1.2,
      wps
        .getDataForWay(cyclewaySegregatedCrossingWithTrafficLights)
        .getWalkSafetyFeatures()
        .forward(),
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
    assertEquals(
      1.3,
      wps.getDataForWay(wayWithMixins).getBicycleSafetyFeatures().forward(),
      epsilon
    );
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit > 35 and <= 60 kph
    assertEquals(1.6, wps.getDataForWay(wayWithMixins).getWalkSafetyFeatures().forward(), epsilon);

    OSMWithTags wayWithMixinsAndCustomSafety = new OSMWithTags();
    // highway=service has custom bicycle safety of 1.1 but no custom walk safety
    wayWithMixinsAndCustomSafety.addTag("highway", "service");
    // surface has mixin bicycle safety of 1.3 but no walk safety
    wayWithMixinsAndCustomSafety.addTag("surface", "metal");
    // 1.1 * 1.3 = 1.43
    assertEquals(
      1.43,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).getBicycleSafetyFeatures().forward(),
      epsilon
    );
    // 1.6 is the default walk safety for a way with ALL permissions and speed limit <= 35 kph
    assertEquals(
      1.45,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).getWalkSafetyFeatures().forward(),
      epsilon
    );
  }
}
