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
    OSMWithTags lamarTunnel = new OSMWithTags();
    lamarTunnel.addTag("highway", "footway");
    lamarTunnel.addTag("indoor", "yes");
    lamarTunnel.addTag("lit", "yes");
    lamarTunnel.addTag("name", "Lamar Tunnel");
    lamarTunnel.addTag("tunnel", "yes");

    assertEquals(StreetTraversalPermission.NONE, wps.getDataForWay(lamarTunnel).getPermission());
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

    assertEquals(StreetTraversalPermission.CAR, wps.getDataForWay(tunnel).getPermission());
  }
}
