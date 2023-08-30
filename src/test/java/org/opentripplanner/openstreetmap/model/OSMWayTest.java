package org.opentripplanner.openstreetmap.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.graph_builder.module.osm.OsmFilter;
import org.opentripplanner.graph_builder.module.osm.StreetTraversalPermissionPair;
import org.opentripplanner.openstreetmap.wayproperty.WayProperties;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class OSMWayTest {

  @Test
  void testIsBicycleDismountForced() {
    OSMWay way = new OSMWay();
    assertFalse(way.isBicycleDismountForced());

    way.addTag("bicycle", "dismount");
    assertTrue(way.isBicycleDismountForced());
  }

  @Test
  void testIsSteps() {
    OSMWay way = new OSMWay();
    assertFalse(way.isSteps());

    way.addTag("highway", "primary");
    assertFalse(way.isSteps());

    way.addTag("highway", "steps");
    assertTrue(way.isSteps());
  }

  @Test
  void wheelchairAccessibleStairs() {
    var osm1 = new OSMWay();
    osm1.addTag("highway", "steps");
    assertFalse(osm1.isWheelchairAccessible());

    // explicitly suitable for wheelchair users, perhaps because of a ramp
    var osm2 = new OSMWay();
    osm2.addTag("highway", "steps");
    osm2.addTag("wheelchair", "yes");
    assertTrue(osm2.isWheelchairAccessible());
  }

  @Test
  void testIsRoundabout() {
    OSMWay way = new OSMWay();
    assertFalse(way.isRoundabout());

    way.addTag("junction", "dovetail");
    assertFalse(way.isRoundabout());

    way.addTag("junction", "roundabout");
    assertTrue(way.isRoundabout());
  }

  @Test
  void testIsOneWayDriving() {
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
  void testIsOneWayBicycle() {
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
  void testIsOneDirectionSidepath() {
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
  void testIsOpposableCycleway() {
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
  void testCarPermission() {
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
  void testMotorCarTagAllowedPermissions() {
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
  void testMotorCarTagDeniedPermissions() {
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
  void testMotorVehicleTagAllowedPermissions() {
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
  void testMotorVehicleTagDeniedPermissions() {
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
  void testSidepathPermissions() {
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

  @Test
  void escalator() {
    assertFalse(WayTestData.cycleway().isEscalator());

    var escalator = new OSMWay();
    escalator.addTag("highway", "steps");
    assertFalse(escalator.isEscalator());

    escalator.addTag("conveying", "yes");
    assertTrue(escalator.isEscalator());

    escalator.addTag("conveying", "whoknows?");
    assertFalse(escalator.isEscalator());
  }

  private StreetTraversalPermissionPair getWayProperties(OSMWay way) {
    WayPropertySet wayPropertySet = new WayPropertySet();
    WayProperties wayData = wayPropertySet.getDataForWay(way);

    StreetTraversalPermission permissions = OsmFilter.getPermissionsForWay(
      way,
      wayData.getPermission()
    );
    return OsmFilter.getPermissions(permissions, way);
  }
}
