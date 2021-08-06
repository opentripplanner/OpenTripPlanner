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
    var way = new OSMWithTags();
    way.addTag("highway", "path");
    way.addTag("mtb:scale", "3");

    assertEquals(
        wps.getDataForWay(way).getPermission(), StreetTraversalPermission.NONE);
  }

  @Test
  public void testMtbScalePedestrian() {
    var way = new OSMWithTags();
    way.addTag("highway", "path");
    way.addTag("mtb:scale", "1");

    assertEquals(
        wps.getDataForWay(way).getPermission(), StreetTraversalPermission.PEDESTRIAN);
  }
}
