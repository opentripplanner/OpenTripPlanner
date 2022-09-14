package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class FinlandWayPropertySetSourceTest {

  static WayPropertySet wps = new WayPropertySet();
  static float epsilon = 0.01f;

  static {
    var source = new FinlandWayPropertySetSource();
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
    OSMWithTags footway = new OSMWithTags();
    footway.addTag("highway", "footway");
    OSMWithTags sidewalk = new OSMWithTags();
    sidewalk.addTag("footway", "sidewalk");
    sidewalk.addTag("highway", "footway");
    assertEquals(2.06, wps.getDataForWay(primaryWay).getBicycleSafetyFeatures().forward(), epsilon);
    assertEquals(1.8, wps.getDataForWay(primaryWay).getWalkSafetyFeatures().forward(), epsilon);
    assertEquals(1.1, wps.getDataForWay(footway).getWalkSafetyFeatures().forward(), epsilon);
    assertEquals(1.0, wps.getDataForWay(sidewalk).getWalkSafetyFeatures().forward(), epsilon);
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
    // 1.8 is the default walk safety for a way with ALL permissions
    assertEquals(1.8, wps.getDataForWay(wayWithMixins).getWalkSafetyFeatures().forward(), epsilon);

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
    // 1.8 is the default walk safety for a way with ALL permissions
    assertEquals(
      1.8,
      wps.getDataForWay(wayWithMixinsAndCustomSafety).getWalkSafetyFeatures().forward(),
      epsilon
    );
  }
}
