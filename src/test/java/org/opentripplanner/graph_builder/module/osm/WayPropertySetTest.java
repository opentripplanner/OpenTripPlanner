package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.CAR;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.NONE;

import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

class WayPropertySetTest {

  @Test
  public void carTunnel() {
    WayPropertySet wps = wps();

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

    assertEquals(CAR, wps.getDataForWay(tunnel).getPermission());
  }

  @Test
  void pedestrianTunnelSpecificity() {
    WayPropertySet wps = wps();

    // https://www.openstreetmap.org/way/127288293
    OSMWithTags tunnel1 = new OSMWithTags();
    tunnel1.addTag("highway", "footway");
    tunnel1.addTag("indoor", "yes");
    tunnel1.addTag("layer", "-1");
    tunnel1.addTag("lit", "yes");
    tunnel1.addTag("name", "Lamar Tunnel");
    tunnel1.addTag("tunnel", "yes");

    assertEquals(NONE, wps.getDataForWay(tunnel1).getPermission());
  }

  @Nonnull
  private static WayPropertySet wps() {
    var wps = new WayPropertySet();
    var source = new WayPropertySetSource() {
      @Override
      public void populateProperties(WayPropertySet props) {
        props.setProperties("highway=primary", withModes(CAR));
        props.setProperties("highway=footway;layer=-1;tunnel=yes;indoor=yes", withModes(NONE));
      }
    };
    source.populateProperties(wps);
    return wps;
  }
}
