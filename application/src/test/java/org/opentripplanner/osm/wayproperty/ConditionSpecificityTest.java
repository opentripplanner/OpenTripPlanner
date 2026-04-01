package org.opentripplanner.osm.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;
import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmEntityForTest;
import org.opentripplanner.osm.wayproperty.specifier.ExactMatchSpecifier;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

class ConditionSpecificityTest {

  private static final float EPSILON = 0.001f;

  @Test
  public void carTunnel() {
    var tunnel = WayTestData.carTunnel();
    WayPropertySet wps = wps();
    assertEquals(CAR, wps.getDataForWay(tunnel).forward().getPermission());
  }

  @Test
  public void carMaxSpeed() {
    var motorWaySpeed = 35f;
    WayPropertySetBuilder builder = WayPropertySet.of();
    builder.setCarSpeed("highway=motorway", motorWaySpeed);
    WayPropertySet wps = builder.build();

    // Test that there are default values
    assertEquals(38f, wps.maxPossibleCarSpeed(), EPSILON);

    // Speed limit that is within limits should be used as the max used car speed
    OsmEntity streetWithSpeedLimit = new OsmEntityForTest();
    streetWithSpeedLimit.addTag("highway", "motorway");
    streetWithSpeedLimit.addTag("maxspeed", "120");
    var waySpeed = wps.getCarSpeedForWay(streetWithSpeedLimit, FORWARD);
    assertEquals(33.33336, waySpeed, EPSILON);

    // Speed limit that is higher than maxPossibleCarSpeed should be ignored and regular motorway
    // speed limit should be used instead
    OsmEntity streetWithTooHighSpeedLimit = new OsmEntityForTest();
    streetWithTooHighSpeedLimit.addTag("highway", "motorway");
    streetWithTooHighSpeedLimit.addTag("maxspeed", "200");
    waySpeed = wps.getCarSpeedForWay(streetWithTooHighSpeedLimit, FORWARD);
    assertEquals(motorWaySpeed, waySpeed, EPSILON);

    // Speed limit that is too low should be ignored and regular motorway speed limit should
    // be used instead
    OsmEntity streetWithTooLowSpeedLimit = new OsmEntityForTest();
    streetWithTooLowSpeedLimit.addTag("highway", "motorway");
    streetWithTooLowSpeedLimit.addTag("maxspeed", "0");
    waySpeed = wps.getCarSpeedForWay(streetWithTooLowSpeedLimit, FORWARD);
    assertEquals(motorWaySpeed, waySpeed, EPSILON);
  }

  @Test
  void pedestrianTunnelSpecificity() {
    var tunnel = WayTestData.pedestrianTunnel();
    WayPropertySet wps = wps();
    assertEquals(NONE, wps.getDataForEntity(tunnel).getPermission());
  }

  @Test
  void mixinLeftSide() {
    var cycleway = WayTestData.cyclewayLeft();
    WayPropertySet wps = wps();
    assertEquals(1, wps.getDataForWay(cycleway).forward().bicycleSafety());
    assertEquals(5, wps.getDataForWay(cycleway).backward().bicycleSafety());
  }

  private static WayPropertySet wps() {
    var props = WayPropertySet.of();
    props.setProperties("highway=primary", withModes(CAR));
    props.setProperties(
      new ExactMatchSpecifier("highway=footway;layer=-1;tunnel=yes;indoor=yes"),
      withModes(NONE)
    );
    props.setMixinProperties("cycleway=lane", ofBicycleSafety(5));
    return props.build();
  }
}
