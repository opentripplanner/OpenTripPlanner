package org.opentripplanner.openstreetmap.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OSMFilter;
import org.opentripplanner.graph_builder.module.osm.StreetTraversalPermissionPair;
import org.opentripplanner.graph_builder.module.osm.WayProperties;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class OSMWayTest {

  @Test
  public void testIsBicycleDismountForced() {
    OSMWay way = new OSMWay();
    assertFalse(way.isBicycleDismountForced());

    way.addTag("bicycle", "dismount");
    assertTrue(way.isBicycleDismountForced());
  }

  @Test
  public void testIsSteps() {
    OSMWay way = new OSMWay();
    assertFalse(way.isSteps());

    way.addTag("highway", "primary");
    assertFalse(way.isSteps());

    way.addTag("highway", "steps");
    assertTrue(way.isSteps());
  }

  @Test
  public void testIsRoundabout() {
    OSMWay way = new OSMWay();
    assertFalse(way.isRoundabout());

    way.addTag("junction", "dovetail");
    assertFalse(way.isRoundabout());

    way.addTag("junction", "roundabout");
    assertTrue(way.isRoundabout());
  }

  @Test
  public void testIsOneWayDriving() {
    OSMWay way = new OSMWay();
    assertFalse(way.isOneWayForwardDriving());
    assertFalse(way.isOneWayReverseDriving());

    way.addTag("oneway", "notatagvalue");
    assertFalse(way.isOneWayForwardDriving());
    assertFalse(way.isOneWayReverseDriving());

    way.addTag("oneway", "1");
    assertTrue(way.isOneWayForwardDriving());
    assertFalse(way.isOneWayReverseDriving());

    way.addTag("oneway", "-1");
    assertFalse(way.isOneWayForwardDriving());
    assertTrue(way.isOneWayReverseDriving());
  }

  @Test
  public void testIsOneWayBicycle() {
    OSMWay way = new OSMWay();
    assertFalse(way.isOneWayForwardBicycle());
    assertFalse(way.isOneWayReverseBicycle());

    way.addTag("oneway:bicycle", "notatagvalue");
    assertFalse(way.isOneWayForwardBicycle());
    assertFalse(way.isOneWayReverseBicycle());

    way.addTag("oneway:bicycle", "1");
    assertTrue(way.isOneWayForwardBicycle());
    assertFalse(way.isOneWayReverseBicycle());

    way.addTag("oneway:bicycle", "-1");
    assertFalse(way.isOneWayForwardBicycle());
    assertTrue(way.isOneWayReverseBicycle());
  }

  @Test
  public void testIsOneDirectionSidepath() {
    OSMWay way = new OSMWay();
    assertFalse(way.isForwardDirectionSidepath());
    assertFalse(way.isReverseDirectionSidepath());

    way.addTag("bicycle:forward", "use_sidepath");
    assertTrue(way.isForwardDirectionSidepath());
    assertFalse(way.isReverseDirectionSidepath());

    way.addTag("bicycle:backward", "use_sidepath");
    assertTrue(way.isForwardDirectionSidepath());
    assertTrue(way.isReverseDirectionSidepath());
  }

  @Test
  public void testIsOpposableCycleway() {
    OSMWay way = new OSMWay();
    assertFalse(way.isOpposableCycleway());

    way.addTag("cycleway", "notatagvalue");
    assertFalse(way.isOpposableCycleway());

    way.addTag("cycleway", "oppo");
    assertFalse(way.isOpposableCycleway());

    way.addTag("cycleway", "opposite");
    assertTrue(way.isOpposableCycleway());

    way.addTag("cycleway", "nope");
    way.addTag("cycleway:left", "opposite_side");
    assertTrue(way.isOpposableCycleway());
  }

  /**
   * Tests if cars can drive on unclassified highways with bicycleDesignated
   * <p>
   * Check for bug #1878 and PR #1880
   */
  @Test
  public void testCarPermission() {
    OSMWay way = new OSMWay();
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
  public void testMotorCarTagAllowedPermissions() {
    OSMWay way = new OSMWay();
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
  public void testMotorCarTagDeniedPermissions() {
    OSMWay way = new OSMWay();
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
  public void testMotorVehicleTagAllowedPermissions() {
    OSMWay way = new OSMWay();
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
  public void testMotorVehicleTagDeniedPermissions() {
    OSMWay way = new OSMWay();
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

  @Test
  public void testSidepathPermissions() {
    OSMWay way = new OSMWay();
    way.addTag("bicycle", "use_sidepath");
    way.addTag("highway", "primary");
    way.addTag("lanes", "2");
    way.addTag("maxspeed", "70");
    way.addTag("oneway", "yes");
    var permissionPair = getWayProperties(way);

    assertFalse(permissionPair.main().allows(StreetTraversalPermission.BICYCLE));
    assertFalse(permissionPair.back().allows(StreetTraversalPermission.BICYCLE));

    assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));
    assertFalse(permissionPair.back().allows(StreetTraversalPermission.CAR));

    way = new OSMWay();
    way.addTag("bicycle:forward", "use_sidepath");
    way.addTag("highway", "tertiary");
    permissionPair = getWayProperties(way);

    assertFalse(permissionPair.main().allows(StreetTraversalPermission.BICYCLE));
    assertTrue(permissionPair.back().allows(StreetTraversalPermission.BICYCLE));

    assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));
    assertTrue(permissionPair.back().allows(StreetTraversalPermission.CAR));

    way = new OSMWay();
    way.addTag("bicycle:backward", "use_sidepath");
    way.addTag("highway", "tertiary");
    permissionPair = getWayProperties(way);

    assertTrue(permissionPair.main().allows(StreetTraversalPermission.BICYCLE));
    assertFalse(permissionPair.back().allows(StreetTraversalPermission.BICYCLE));

    assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));
    assertTrue(permissionPair.back().allows(StreetTraversalPermission.CAR));

    way = new OSMWay();
    way.addTag("highway", "tertiary");
    way.addTag("oneway", "yes");
    way.addTag("oneway:bicycle", "no");
    permissionPair = getWayProperties(way);

    assertTrue(permissionPair.main().allows(StreetTraversalPermission.BICYCLE));
    assertTrue(permissionPair.back().allows(StreetTraversalPermission.BICYCLE));

    assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));
    assertFalse(permissionPair.back().allows(StreetTraversalPermission.CAR));

    way.addTag("bicycle:forward", "use_sidepath");
    permissionPair = getWayProperties(way);
    assertFalse(permissionPair.main().allows(StreetTraversalPermission.BICYCLE));
    assertTrue(permissionPair.back().allows(StreetTraversalPermission.BICYCLE));

    assertTrue(permissionPair.main().allows(StreetTraversalPermission.CAR));
    assertFalse(permissionPair.back().allows(StreetTraversalPermission.CAR));
  }

  private StreetTraversalPermissionPair getWayProperties(OSMWay way) {
    WayPropertySet wayPropertySet = new WayPropertySet();
    WayProperties wayData = wayPropertySet.getDataForWay(way);

    StreetTraversalPermission permissions = OSMFilter.getPermissionsForWay(
      way,
      wayData.getPermission()
    );
    return OSMFilter.getPermissions(permissions, way);
  }
}
