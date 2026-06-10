package org.opentripplanner.osm.tagmapping;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.osm.model.TraverseDirection.BACKWARD;
import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmTestEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

class OsmTagMapperTest {

  private static final Locale FI = Locale.of("FI");
  private static final WayPropertySet WPS = new OsmTagMapper().buildWayPropertySet();
  private static final float EPSILON = 0.01f;

  @Test
  void isMotorThroughTrafficExplicitlyDisallowed() {
    var o = OsmWay.of().build();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o = o.copy().withTag("access", "something").build();
    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o = o.copy().withTag("access", "destination").build();
    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o = o.copy().withTag("access", "private").build();
    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    assertTrue(
      osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(
        way("motor_vehicle", "destination")
      )
    );
  }

  @Test
  void isBicycleThroughTrafficExplicitlyDisallowed() {
    OsmTagMapper osmTagMapper = new OsmTagMapper();
    assertTrue(
      osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(way("bicycle", "destination"))
    );
    assertTrue(
      osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(way("access", "destination"))
    );
  }

  @Test
  void isWalkThroughTrafficExplicitlyDisallowed() {
    OsmTagMapper osmTagMapper = new OsmTagMapper();
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(way("foot", "destination")));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(way("access", "destination")));
  }

  @Test
  void testAccessNo() {
    OsmTestEntity tags = new OsmTestEntity("access", "no");
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testAccessPrivate() {
    OsmTestEntity tags = new OsmTestEntity("access", "private");
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testFootway() {
    OsmWay footway = WayTestData.footway();
    assertEquals(PEDESTRIAN, WPS.getDataForEntity(footway).getPermission());
  }

  @Test
  void indoor() {
    var corridor = WPS.getDataForEntity(WayTestData.indoor("corridor"));
    assertEquals(PEDESTRIAN, corridor.getPermission());
    var area = WPS.getDataForEntity(WayTestData.indoor("area"));
    assertEquals(PEDESTRIAN, area.getPermission());
  }

  @Test
  void testFootwaySharedWithBicycle() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(WayTestData.footwaySharedWithBicycle()).getPermission()
    );
  }

  @Test
  void testIllegalCyclewaySharedWithFootWithFootwayTag() {
    var way = WayTestData.cyclewaySharedWithFoot().copy().withTag("footway", "sidewalk").build();
    assertEquals(PEDESTRIAN_AND_BICYCLE, WPS.getDataForEntity(way).getPermission());
    assertEquals(
      WPS.getDataForEntity(WayTestData.cycleway()).bicycleSafety(),
      WPS.getDataForEntity(way).bicycleSafety()
    );
  }

  @Test
  void testCycleway() {
    assertEquals(BICYCLE, WPS.getDataForEntity(WayTestData.cycleway()).getPermission());
  }

  @Test
  void testCyclewaySharedWithFoot() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(WayTestData.cyclewaySharedWithFoot()).getPermission()
    );
  }

  @Test
  void testPedestrian() {
    assertEquals(PEDESTRIAN, WPS.getDataForEntity(WayTestData.pedestrianArea()).getPermission());
  }

  @Test
  void testBridleway() {
    assertEquals(NONE, WPS.getDataForEntity(WayTestData.bridleway()).getPermission());
  }

  @Test
  void testPath() {
    assertEquals(PEDESTRIAN_AND_BICYCLE, WPS.getDataForEntity(WayTestData.path()).getPermission());
  }

  @Test
  void testBridlewaySharedWithFootAndBicycle() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(WayTestData.bridlewaySharedWithFootAndBicycle()).getPermission()
    );
  }

  @Test
  void testMotorway() {
    assertEquals(CAR, WPS.getDataForWay(WayTestData.motorway()).forward().getPermission());
    assertEquals(NONE, WPS.getDataForWay(WayTestData.motorway()).backward().getPermission());
  }

  @Test
  void testMotorwayWithBicycleAllowed() {
    assertEquals(
      BICYCLE_AND_CAR,
      WPS.getDataForWay(WayTestData.motorwayWithBicycleAllowed()).forward().getPermission()
    );
    assertEquals(
      NONE,
      WPS.getDataForWay(WayTestData.motorwayWithBicycleAllowed()).backward().getPermission()
    );
  }

  @Test
  void testPrimaryMotorroad() {
    assertEquals(
      CAR,
      WPS.getDataForEntity(WayTestData.highwayPrimaryWithMotorroad()).getPermission()
    );
  }

  @Test
  void testTrunk() {
    assertEquals(ALL, WPS.getDataForEntity(WayTestData.highwayTrunk()).getPermission());
  }

  @Test
  void testTrunkMotorroad() {
    assertEquals(
      CAR,
      WPS.getDataForEntity(WayTestData.highwayTrunkWithMotorroad()).getPermission()
    );
  }

  @Test
  void testTrunkWalkSafety() {
    var rawScore = WPS.getDataForWay(WayTestData.highwayTrunk()).forward().walkSafety();
    var scoreWithLane = WPS.getDataForWay(
      WayTestData.highwayTrunk().copy().withTag("sidewalk", "lane").build()
    )
      .forward()
      .walkSafety();
    var scoreWithSidewalk = WPS.getDataForWay(
      WayTestData.highwayTrunk().copy().withTag("sidewalk", "both").build()
    )
      .forward()
      .walkSafety();
    var scoreWithSeparateSidewalk = WPS.getDataForWay(
      WayTestData.highwayTrunk().copy().withTag("sidewalk", "separate").build()
    )
      .forward()
      .walkSafety();
    assertTrue(rawScore > 5);
    assertTrue(scoreWithLane < rawScore);
    assertTrue(scoreWithSidewalk < scoreWithLane);
    assertEquals(rawScore, scoreWithSeparateSidewalk);
  }

  @Test
  void testTertiary() {
    assertEquals(ALL, WPS.getDataForEntity(WayTestData.highwayTertiary()).getPermission());
  }

  @Test
  void testFootModifier() {
    OsmTagMapper osmTagMapper = new OsmTagMapper();
    OsmTestEntity tags = new OsmTestEntity(entry("access", "private"), entry("foot", "yes"));

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testVehicleDenied() {
    OsmTestEntity tags = new OsmTestEntity("vehicle", "destination");
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testVehicleDeniedMotorVehiclePermissive() {
    OsmTestEntity tags = new OsmTestEntity(
      entry("vehicle", "destination"),
      entry("motor_vehicle", "designated")
    );
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testVehicleDeniedBicyclePermissive() {
    OsmTestEntity tags = new OsmTestEntity(
      entry("vehicle", "destination"),
      entry("bicycle", "designated")
    );
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testMotorcycleModifier() {
    OsmTestEntity tags = new OsmTestEntity(
      entry("access", "private"),
      entry("motor_vehicle", "yes")
    );
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testBicycleModifier() {
    OsmTestEntity tags = new OsmTestEntity(entry("access", "private"), entry("bicycle", "yes"));
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testBicyclePermissive() {
    OsmTestEntity tags = new OsmTestEntity(
      entry("access", "private"),
      entry("bicycle", "permissive")
    );
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testOneWay() {
    var way = WayTestData.highwayTertiary().copy().withTag("oneway", "yes").build();
    var props = WPS.getDataForWay(way);
    assertEquals(ALL, props.forward().getPermission());
    assertEquals(PEDESTRIAN, props.backward().getPermission());

    var wayWithBicycle = way.copy().withTag("oneway:bicycle", "no").build();
    props = WPS.getDataForWay(wayWithBicycle);
    assertEquals(ALL, props.forward().getPermission());
    assertEquals(PEDESTRIAN_AND_BICYCLE, props.backward().getPermission());
  }

  public static List<OsmEntity> roadCases() {
    return List.of(
      WayTestData.carTunnel(),
      WayTestData.southwestMayoStreet(),
      WayTestData.southeastLaBonitaWay(),
      WayTestData.fiveLanes(),
      WayTestData.highwayTertiary()
    );
  }

  @ParameterizedTest
  @MethodSource("roadCases")
  void motorroad(OsmWay way) {
    assertEquals(ALL, WPS.getDataForWay(way).forward().getPermission());

    var wayWithMotorroad = way.copy().withTag("motorroad", "yes").build();
    assertEquals(CAR, WPS.getDataForWay(wayWithMotorroad).forward().getPermission());
  }

  @Test
  void corridorName() {
    var way = way("highway", "corridor");
    assertEquals("corridor", WPS.getCreativeName(way).toString());
    assertEquals("Korridor", WPS.getCreativeName(way).toString(Locale.GERMANY));
    assertEquals("käytävä", WPS.getCreativeName(way).toString(FI));
  }

  @Test
  void indoorAreaName() {
    var way = way("indoor", "area");
    assertEquals("indoor area", WPS.getCreativeName(way).toString());
    assertEquals("Innenbereich", WPS.getCreativeName(way).toString(Locale.GERMANY));
    assertEquals("sisätila", WPS.getCreativeName(way).toString(FI));
  }

  @Test
  void stairs() {
    // there is no special handling for stairs with ramps yet
    var props = WPS.getDataForWay(WayTestData.stairs());
    assertEquals(PEDESTRIAN, props.forward().getPermission());
    assertEquals(PEDESTRIAN, props.backward().getPermission());
  }

  @Test
  void footDiscouraged() {
    var regular = WayTestData.highwayTertiary();
    var props = WPS.getDataForWay(regular);
    assertEquals(ALL, props.forward().getPermission());
    assertEquals(1, props.forward().walkSafety());
    assertEquals(ALL, props.backward().getPermission());
    assertEquals(1, props.backward().walkSafety());

    var discouraged = WayTestData.highwayTertiary().copy().withTag("foot", "discouraged").build();
    var discouragedProps = WPS.getDataForWay(discouraged);
    assertEquals(ALL, discouragedProps.forward().getPermission());
    assertEquals(3, discouragedProps.forward().walkSafety());
    assertEquals(ALL, discouragedProps.backward().getPermission());
    assertEquals(3, discouragedProps.backward().walkSafety());
  }

  @Test
  void bicycleDiscouraged() {
    var regular = WayTestData.southeastLaBonitaWay();
    var props = WPS.getDataForWay(regular);
    assertEquals(ALL, props.forward().getPermission());
    assertEquals(.98, props.forward().bicycleSafety());
    assertEquals(ALL, props.backward().getPermission());
    assertEquals(.98, props.backward().bicycleSafety());

    var discouraged = WayTestData.southeastLaBonitaWay()
      .copy()
      .withTag("bicycle", "discouraged")
      .build();
    var discouragedProps = WPS.getDataForWay(discouraged);
    assertEquals(ALL, discouragedProps.forward().getPermission());
    assertEquals(2.94, discouragedProps.forward().bicycleSafety(), EPSILON);
    assertEquals(ALL, discouragedProps.backward().getPermission());
    assertEquals(2.94, discouragedProps.backward().bicycleSafety(), EPSILON);
  }

  @Test
  void footUseSidepath() {
    var regular = WayTestData.highwayTertiary();
    var props = WPS.getDataForWay(regular);
    assertEquals(ALL, props.forward().getPermission());
    assertEquals(1, props.forward().walkSafety());
    assertEquals(ALL, props.backward().getPermission());
    assertEquals(1, props.backward().walkSafety());

    var useSidepath = WayTestData.highwayTertiary().copy().withTag("foot", "use_sidepath").build();
    var useSidepathProps = WPS.getDataForWay(useSidepath);
    assertEquals(ALL, useSidepathProps.forward().getPermission());
    assertEquals(5, useSidepathProps.forward().walkSafety());
    assertEquals(ALL, useSidepathProps.backward().getPermission());
    assertEquals(5, useSidepathProps.backward().walkSafety());
  }

  @Test
  void bicycleUseSidepath() {
    var regular = WayTestData.southeastLaBonitaWay();
    var props = WPS.getDataForWay(regular);
    assertEquals(ALL, props.forward().getPermission());
    assertEquals(.98, props.forward().bicycleSafety());
    assertEquals(ALL, props.backward().getPermission());
    assertEquals(.98, props.backward().bicycleSafety());

    var useSidepath = WayTestData.southeastLaBonitaWay()
      .copy()
      .withTag("bicycle", "use_sidepath")
      .build();
    var useSidepathProps = WPS.getDataForWay(useSidepath);
    assertEquals(ALL, useSidepathProps.forward().getPermission());
    assertEquals(4.9, useSidepathProps.forward().bicycleSafety(), EPSILON);
    assertEquals(ALL, useSidepathProps.backward().getPermission());
    assertEquals(4.9, useSidepathProps.backward().bicycleSafety(), EPSILON);

    var useSidepathForward = WayTestData.southeastLaBonitaWay()
      .copy()
      .withTag("bicycle:forward", "use_sidepath")
      .build();
    var useSidepathForwardProps = WPS.getDataForWay(useSidepathForward);
    assertEquals(ALL, useSidepathForwardProps.forward().getPermission());
    assertEquals(ALL, useSidepathForwardProps.backward().getPermission());
    assertEquals(4.9, useSidepathForwardProps.forward().bicycleSafety(), EPSILON);
    assertEquals(0.98, useSidepathForwardProps.backward().bicycleSafety(), EPSILON);

    var useSidepathBackward = WayTestData.southeastLaBonitaWay()
      .copy()
      .withTag("bicycle:backward", "use_sidepath")
      .build();
    var useSidepathBackwardProps = WPS.getDataForWay(useSidepathBackward);
    assertEquals(ALL, useSidepathBackwardProps.forward().getPermission());
    assertEquals(ALL, useSidepathBackwardProps.backward().getPermission());
    assertEquals(0.98, useSidepathBackwardProps.forward().bicycleSafety(), EPSILON);
    assertEquals(4.9, useSidepathBackwardProps.backward().bicycleSafety(), EPSILON);
  }

  @Test
  void slopeOverrides() {
    var regular = WayTestData.southeastLaBonitaWay();
    assertFalse(WPS.getSlopeOverride(regular));

    var indoor = WayTestData.southeastLaBonitaWay().copy().withTag("indoor", "yes").build();
    assertTrue(WPS.getSlopeOverride(indoor));
  }

  /**
   * Test that car speeds are calculated accurately
   */
  @Test
  public void testCarSpeeds() {
    OsmWay way;

    way = OsmWay.of().withTag("maxspeed", "60").build();
    assertTrue(within(kmhAsMs(60), WPS.getCarSpeedForWay(way, FORWARD), EPSILON));
    assertTrue(within(kmhAsMs(60), WPS.getCarSpeedForWay(way, BACKWARD), EPSILON));

    way = OsmWay.of()
      .withTag("maxspeed:forward", "80")
      .withTag("maxspeed:backward", "20")
      .withTag("maxspeed", "40")
      .build();
    assertTrue(within(kmhAsMs(80), WPS.getCarSpeedForWay(way, FORWARD), EPSILON));
    assertTrue(within(kmhAsMs(20), WPS.getCarSpeedForWay(way, BACKWARD), EPSILON));

    way = OsmWay.of().withTag("maxspeed", "40").withTag("maxspeed:lanes", "60|80|40").build();
    assertTrue(within(kmhAsMs(80), WPS.getCarSpeedForWay(way, FORWARD), EPSILON));
    assertTrue(within(kmhAsMs(80), WPS.getCarSpeedForWay(way, BACKWARD), EPSILON));

    way = OsmWay.of().withTag("maxspeed", "20").withTag("maxspeed:motorcar", "80").build();
    assertTrue(within(kmhAsMs(80), WPS.getCarSpeedForWay(way, FORWARD), EPSILON));
    assertTrue(within(kmhAsMs(80), WPS.getCarSpeedForWay(way, BACKWARD), EPSILON));

    // test with english units
    way = OsmWay.of().withTag("maxspeed", "35 mph").build();
    assertTrue(within(kmhAsMs(35 * 1.609f), WPS.getCarSpeedForWay(way, FORWARD), EPSILON));
    assertTrue(within(kmhAsMs(35 * 1.609f), WPS.getCarSpeedForWay(way, BACKWARD), EPSILON));
  }

  /**
   * Convert kilometers per hour to meters per second
   */
  private float kmhAsMs(float kmh) {
    return (kmh * 1000) / 3600;
  }

  public OsmEntity way(String key, String value) {
    return OsmWay.of().withTag(key, value).build();
  }

  /**
   * Test that two values are within epsilon of each other.
   */
  private boolean within(float val1, float val2, float epsilon) {
    return (Math.abs(val1 - val2) < epsilon);
  }
}
