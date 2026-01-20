package org.opentripplanner.osm.wayproperty;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.StreetTraversalPermissionPair;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.StreetTraversalPermission;

class OverridePermissionsTest {

  public static final WayPropertySet WAY_PROPERTY_SET = WayPropertySet.of().build();

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
  }

  private StreetTraversalPermissionPair getWayProperties(OsmWay way) {
    WayPropertiesPair wayData = WAY_PROPERTY_SET.getDataForWay(way);

    return new StreetTraversalPermissionPair(
      wayData.forward().getPermission(),
      wayData.backward().getPermission()
    );
  }
}
