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
    OsmWay way = OsmWay.of().withTag("highway", "unclassified").build();

    var permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

    way = way.copy().withTag("bicycle", "designated").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));
  }

  /**
   * Tests that motorcar/bicycle/foot private don't add permissions but yes add permission if access
   * is no
   */
  @Test
  void testMotorCarTagAllowedPermissions() {
    OsmWay way = OsmWay.of().withTag("highway", "residential").build();
    var permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

    way = way.copy().withTag("access", "no").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allowsNothing());

    way = way
      .copy()
      .withTag("motorcar", "private")
      .withTag("bicycle", "private")
      .withTag("foot", "private")
      .build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allowsNothing());

    way = way.copy().withTag("motorcar", "yes").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));

    way = way.copy().withTag("bicycle", "yes").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.BICYCLE_AND_CAR));

    way = way.copy().withTag("foot", "yes").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));
  }

  /**
   * Tests that motorcar/bicycle/foot private don't add permissions but no remove permission if
   * access is yes
   */
  @Test
  void testMotorCarTagDeniedPermissions() {
    OsmWay way = OsmWay.of().withTag("highway", "residential").build();
    var permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

    way = way.copy().withTag("motorcar", "no").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));

    way = way.copy().withTag("bicycle", "no").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.PEDESTRIAN));

    way = way.copy().withTag("foot", "no").build();
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
    OsmWay way = OsmWay.of().withTag("highway", "residential").build();
    var permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

    way = way.copy().withTag("access", "no").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allowsNothing());

    way = way
      .copy()
      .withTag("motor_vehicle", "private")
      .withTag("bicycle", "private")
      .withTag("foot", "private")
      .build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allowsNothing());

    way = way.copy().withTag("motor_vehicle", "yes").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));

    way = way.copy().withTag("bicycle", "yes").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.BICYCLE_AND_CAR));

    way = way.copy().withTag("foot", "yes").build();
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
    OsmWay way = OsmWay.of().withTag("highway", "residential").build();
    var permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.ALL));

    way = way.copy().withTag("motor_vehicle", "no").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE));

    way = way.copy().withTag("bicycle", "no").build();
    permissionPair = getWayProperties(way);
    assertTrue(permissionPair.main().allows(StreetTraversalPermission.PEDESTRIAN));

    way = way.copy().withTag("foot", "no").build();
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
