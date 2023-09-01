package org.opentripplanner.openstreetmap.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.openstreetmap.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapper;
import org.opentripplanner.openstreetmap.wayproperty.specifier.ExactMatchSpecifier;
import org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData;

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

  @Test
  void mixinLeftSide() {
    var cycleway = WayTestData.cyclewayLeft();
    WayPropertySet wps = wps();
    SafetyFeatures expected = new SafetyFeatures(1, 5);
    assertEquals(expected, wps.getDataForWay(cycleway).bicycleSafety());
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
        props.setMixinProperties("cycleway=lane", ofBicycleSafety(5));
      }
    };
    source.populateProperties(wps);
    return wps;
  }
}
