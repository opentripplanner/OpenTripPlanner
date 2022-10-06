package org.opentripplanner.graph_builder.module.osm.specifier;

import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class WayTestData {

  public static OSMWithTags carTunnel() {
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
    return tunnel;
  }

  public static OSMWithTags pedestrianTunnel() {
    // https://www.openstreetmap.org/way/127288293
    OSMWithTags tunnel = new OSMWithTags();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("indoor", "yes");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("name", "Lamar Tunnel");
    tunnel.addTag("tunnel", "yes");
    return tunnel;
  }
}
