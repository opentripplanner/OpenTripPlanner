package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

class OSMSpecifierTest {

  OSMSpecifier highwayPrimary = new OSMSpecifier("highway=primary");
  OSMSpecifier pedestrianUndergroundTunnel = new OSMSpecifier(
    "highway=footway;layer=-1;tunnel=yes;indoor=yes"
  );

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

    var result = highwayPrimary.matchScores(tunnel);
    assertEquals(110, result.first);

    result = pedestrianUndergroundTunnel.matchScores(tunnel);
    assertEquals(0, result.first);
  }

  @Test
  void pedestrianTunnelSpecificity() {
    // https://www.openstreetmap.org/way/127288293
    OSMWithTags tunnel = new OSMWithTags();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("indoor", "yes");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("name", "Lamar Tunnel");
    tunnel.addTag("tunnel", "yes");

    var result = highwayPrimary.matchScores(tunnel);
    assertEquals(0, result.first);

    result = pedestrianUndergroundTunnel.matchScores(tunnel);
    assertEquals(410, result.first);
  }
}
