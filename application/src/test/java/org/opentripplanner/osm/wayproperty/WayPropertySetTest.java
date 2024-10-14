package org.opentripplanner.osm.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.StreetTraversalPermissionPair;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.OsmWithTags;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.specifier.ExactMatchSpecifier;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;
import org.opentripplanner.street.model.StreetTraversalPermission;

class WayPropertySetTest {

  @Nested
  class ConditionSpecificity {

    @Test
    public void carTunnel() {
      OsmWithTags tunnel = WayTestData.carTunnel();
      WayPropertySet wps = wps();
      assertEquals(CAR, wps.getDataForWay(tunnel).getPermission());
    }

    @Test
    public void carMaxSpeed() {
      var delta = 0.001f;
      var motorWaySpeed = 35f;
      WayPropertySet wps = wps();
      wps.setCarSpeed("highway=motorway", motorWaySpeed);

      // Test that there are default values
      assertEquals(38f, wps.maxPossibleCarSpeed, delta);
      assertEquals(0f, wps.maxUsedCarSpeed, delta);

      // Speed limit that is within limits should be used as the max used car speed
      OsmWithTags streetWithSpeedLimit = new OsmWithTags();
      streetWithSpeedLimit.addTag("highway", "motorway");
      streetWithSpeedLimit.addTag("maxspeed", "120");
      var waySpeed = wps.getCarSpeedForWay(streetWithSpeedLimit, false);
      assertEquals(33.33336, waySpeed, delta);
      assertEquals(33.33336, wps.maxUsedCarSpeed, delta);

      // Speed limit that is higher than maxPossibleCarSpeed should be ignored and regular motorway
      // speed limit should be used instead
      OsmWithTags streetWithTooHighSpeedLimit = new OsmWithTags();
      streetWithTooHighSpeedLimit.addTag("highway", "motorway");
      streetWithTooHighSpeedLimit.addTag("maxspeed", "200");
      waySpeed = wps.getCarSpeedForWay(streetWithTooHighSpeedLimit, false);
      assertEquals(motorWaySpeed, waySpeed, delta);
      assertEquals(motorWaySpeed, wps.maxUsedCarSpeed, delta);

      // Speed limit that is too low should be ignored and regular motorway speed limit should
      // be used instead
      OsmWithTags streetWithTooLowSpeedLimit = new OsmWithTags();
      streetWithTooLowSpeedLimit.addTag("highway", "motorway");
      streetWithTooLowSpeedLimit.addTag("maxspeed", "0");
      waySpeed = wps.getCarSpeedForWay(streetWithTooLowSpeedLimit, false);
      assertEquals(motorWaySpeed, waySpeed, delta);
      assertEquals(motorWaySpeed, wps.maxUsedCarSpeed, delta);
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

  @Nested
  class NoMapper {

    /**
     * Tests if cars can drive on unclassified highways with bicycleDesignated
     * <p>
     * Check for bug #1878 and PR #1880
     */
    @Test
    void testCarPermission() {
      OsmWay way = new OsmWay();
      way.addTag("highway", "unclassified");

      var permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

      way.addTag("bicycle", "designated");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));
    }

    /**
     * Tests that motorcar/bicycle/foot private don't add permissions but yes add permission if access
     * is no
     */
    @Test
    void testMotorCarTagAllowedPermissions() {
      OsmWay way = new OsmWay();
      way.addTag("highway", "residential");
      var permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

      way.addTag("access", "no");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allowsNothing());

      way.addTag("motorcar", "private");
      way.addTag("bicycle", "private");
      way.addTag("foot", "private");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allowsNothing());

      way.addTag("motorcar", "yes");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));

      way.addTag("bicycle", "yes");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.BICYCLE_AND_CAR));

      way.addTag("foot", "yes");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));
    }

    /**
     * Tests that motorcar/bicycle/foot private don't add permissions but no remove permission if
     * access is yes
     */
    @Test
    void testMotorCarTagDeniedPermissions() {
      OsmWay way = new OsmWay();
      way.addTag("highway", "residential");
      var permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

      way.addTag("motorcar", "no");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));

      way.addTag("bicycle", "no");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.PEDESTRIAN));

      way.addTag("foot", "no");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allowsNothing());
      //normal road with specific mode of transport private only is doubtful
      /*way.addTag("motorcar", "private");
        way.addTag("bicycle", "private");
        way.addTag("foot", "private");
        permissionPair = getWayProperties(way);
        assertTrue(permissionPair.main().allowsNothing());*/
    }

    /**
     * Tests that motor_vehicle/bicycle/foot private don't add permissions but yes add permission if
     * access is no
     * <p>
     * Support for motor_vehicle was added in #1881
     */
    @Test
    void testMotorVehicleTagAllowedPermissions() {
      OsmWay way = new OsmWay();
      way.addTag("highway", "residential");
      var permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

      way.addTag("access", "no");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allowsNothing());

      way.addTag("motor_vehicle", "private");
      way.addTag("bicycle", "private");
      way.addTag("foot", "private");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allowsNothing());

      way.addTag("motor_vehicle", "yes");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));

      way.addTag("bicycle", "yes");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.BICYCLE_AND_CAR));

      way.addTag("foot", "yes");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));
    }

    /**
     * Tests that motor_vehicle/bicycle/foot private don't add permissions but no remove permission if
     * access is yes
     * <p>
     * Support for motor_vehicle was added in #1881
     */
    @Test
    void testMotorVehicleTagDeniedPermissions() {
      OsmWay way = new OsmWay();
      way.addTag("highway", "residential");
      var permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

      way.addTag("motor_vehicle", "no");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));

      way.addTag("bicycle", "no");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allows(StreetTraversalPermission.PEDESTRIAN));

      way.addTag("foot", "no");
      permissionPair = getWayProperties(way);
      assertTrue(permissionPair.main().allowsNothing());
      //normal road with specific mode of transport private only is doubtful
      /*way.addTag("motor_vehicle", "private");
        way.addTag("bicycle", "private");
        way.addTag("foot", "private");
        permissionPair = getWayProperties(way);
        assertTrue(permissionPair.main().allowsNothing());*/
    }

    private StreetTraversalPermissionPair getWayProperties(OsmWay way) {
      WayPropertySet wayPropertySet = new WayPropertySet();
      WayProperties wayData = wayPropertySet.getDataForWay(way);

      StreetTraversalPermission def = wayData.getPermission();
      return way.splitPermissions(def);
    }
  }
}
