package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.CAR;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.NONE;

import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.specifier.ExactMatchSpecifier;
import org.opentripplanner.graph_builder.module.osm.specifier.WayTestData;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

class WayPropertySetTest {

  @Test
  public void carTunnel() {
    OSMWithTags tunnel = WayTestData.carTunnel();
    WayPropertySet wps = wps();
    assertEquals(CAR, wps.getDataForWay(tunnel).getPermission());
  }

  @Test
  void pedestrianTunnelSpecificity() {
    var tunnel = WayTestData.pedestrianTunnel();
    WayPropertySet wps = wps();
    assertEquals(NONE, wps.getDataForWay(tunnel).getPermission());
  }

  @Nonnull
  private static WayPropertySet wps() {
    var wps = new WayPropertySet();
    var source = new OsmTagMapper() {
      @Override
      public void populateProperties(WayPropertySet props) {
        props.setProperties("highway=primary", withModes(CAR));
        props.setProperties(
          new ExactMatchSpecifier("highway=footway;layer=-1;tunnel=yes;indoor=yes"),
          withModes(NONE)
        );
      }
    };
    source.populateProperties(wps);
    return wps;
  }
}
