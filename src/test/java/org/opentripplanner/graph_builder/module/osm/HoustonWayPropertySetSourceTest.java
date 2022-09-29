package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

class HoustonWayPropertySetSourceTest {

  static WayPropertySet wps = new WayPropertySet();

  static {
    var source = new HoustonWayPropertySetSource();
    source.populateProperties(wps);
  }

  @Test
  public void lamarTunnel() {
    // https://www.openstreetmap.org/way/127288293
    OSMWithTags tunnel = new OSMWithTags();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("indoor", "yes");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("name", "Lamar Tunnel");
    tunnel.addTag("tunnel", "yes");

    assertEquals(StreetTraversalPermission.NONE, wps.getDataForWay(tunnel).getPermission());
  }

  @Test
  public void harrisCountyTunnel() {
    // https://www.openstreetmap.org/way/127288288
    OSMWithTags tunnel = new OSMWithTags();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("indoor", "yes");
    tunnel.addTag("name", "Harris County Tunnel");
    tunnel.addTag("tunnel", "yes");

    assertEquals(StreetTraversalPermission.PEDESTRIAN, wps.getDataForWay(tunnel).getPermission());
  }

  @Test
  public void carTunnel() {
    // https://www.openstreetmap.org/way/598694756
    OSMWithTags tunnel = new OSMWithTags();
    tunnel.addTag("highway", "primary");
    tunnel.addTag("hov", "lane");
    tunnel.addTag("lanes", "4");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("maxspeed", "30 mph");
    tunnel.addTag("nam", "San Jacinto Street");
    tunnel.addTag("note:lanes", "right lane is hov");
    tunnel.addTag("oneway", "yes");
    tunnel.addTag("surface", "concrete");
    tunnel.addTag("tunnel", "yes");

    assertEquals(StreetTraversalPermission.ALL, wps.getDataForWay(tunnel).getPermission());
  }

  @Test
  public void carUnderpass() {
    // https://www.openstreetmap.org/way/102925214
    OSMWithTags tunnel = new OSMWithTags();
    tunnel.addTag("highway", "motorway_link");
    tunnel.addTag("lanes", "2");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("oneway", "yes");
    tunnel.addTag("tunnel", "yes");

    assertEquals(StreetTraversalPermission.ALL, wps.getDataForWay(tunnel).getPermission());
  }
}
