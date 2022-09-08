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
    assertEquals(2.06, wps.getDataForWay(primaryWay).getBicycleSafetyFeatures().first, epsilon);
    assertEquals(1.5, wps.getDataForWay(primaryWay).getWalkSafetyFeatures().first, epsilon);
    assertEquals(0.8, wps.getDataForWay(footway).getWalkSafetyFeatures().first, epsilon);
    assertEquals(0.7, wps.getDataForWay(sidewalk).getWalkSafetyFeatures().first, epsilon);
  }
}
