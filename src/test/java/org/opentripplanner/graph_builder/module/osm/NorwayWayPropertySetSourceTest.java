package org.opentripplanner.graph_builder.module.osm;

import org.junit.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

import static org.junit.Assert.assertEquals;

public class NorwayWayPropertySetSourceTest {
  static WayPropertySet wps = new WayPropertySet();

  static {
    var source = new NorwayWayPropertySetSource();
    source.populateProperties(wps);
  }

  @Test
  public void testMtbScaleNone() {
    // https://www.openstreetmap.org/way/302610220
    var way1 = new OSMWithTags();
    way1.addTag("highway", "path");
    way1.addTag("mtb:scale", "3");

    assertEquals(
        wps.getDataForWay(way1).getPermission(), StreetTraversalPermission.NONE);

    var way2 = new OSMWithTags();
    way2.addTag("highway", "track");
    way2.addTag("mtb:scale", "3");

    assertEquals(
        wps.getDataForWay(way2).getPermission(), StreetTraversalPermission.NONE);
  }

  @Test
  public void testMtbScalePedestrian() {
    var way1 = new OSMWithTags();
    way1.addTag("highway", "path");
    way1.addTag("mtb:scale", "1");

    assertEquals(
        wps.getDataForWay(way1).getPermission(), StreetTraversalPermission.PEDESTRIAN);

    var way2 = new OSMWithTags();
    way2.addTag("highway", "track");
    way2.addTag("mtb:scale", "1");

    assertEquals(
        wps.getDataForWay(way2).getPermission(), StreetTraversalPermission.PEDESTRIAN);
  }
}
