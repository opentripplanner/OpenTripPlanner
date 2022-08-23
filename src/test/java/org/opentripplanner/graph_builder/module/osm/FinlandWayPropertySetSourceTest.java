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
    OSMWithTags way;
    way = new OSMWithTags();
    way.addTag("highway", "primary");
    way.addTag("oneway", "no");
    assertEquals(2.06, wps.getDataForWay(way).getBicycleSafetyFeatures().first, epsilon);
    assertEquals(2.06, wps.getDataForWay(way).getWalkSafetyFeatures().first, epsilon);
  }
}
