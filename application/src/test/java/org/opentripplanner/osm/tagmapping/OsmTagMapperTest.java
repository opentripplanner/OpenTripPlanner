package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

class OsmTagMapperTest {

  private static final Locale FI = Locale.of("FI");
  private static final WayPropertySet wps = new WayPropertySet();

  static {
    var source = new OsmTagMapper();
    source.populateProperties(wps);
  }

  @Test
  void isMotorThroughTrafficExplicitlyDisallowed() {
    OsmEntity o = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "something");
    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "destination");
    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "private");
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
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "no");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testAccessPrivate() {
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testFootway() {
    OsmWay footway = WayTestData.footway();
    assertEquals(PEDESTRIAN, wps.getDataForEntity(footway).getPermission());
    assertEquals(0.8, wps.getDataForWay(footway).forward().walkSafety());

    footway.addTag("sidewalk", "both");
    assertEquals(0.8, wps.getDataForWay(footway).forward().walkSafety());
  }

  @Test
  void testFootwaySharedWithBicycle() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.footwaySharedWithBicycle()).getPermission()
    );
  }

  @Test
  void testCycleway() {
    assertEquals(BICYCLE, wps.getDataForEntity(WayTestData.cycleway()).getPermission());
  }

  @Test
  void testCyclewaySharedWithFoot() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.cyclewaySharedWithFoot()).getPermission()
    );
  }

  @Test
  void testPedestrian() {
    assertEquals(PEDESTRIAN, wps.getDataForEntity(WayTestData.pedestrianArea()).getPermission());
  }

  @Test
  void testBridleway() {
    assertEquals(NONE, wps.getDataForEntity(WayTestData.bridleway()).getPermission());
  }

  @Test
  void testPath() {
    assertEquals(PEDESTRIAN_AND_BICYCLE, wps.getDataForEntity(WayTestData.path()).getPermission());
  }

  @Test
  void testBridlewaySharedWithFootAndBicycle() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.bridlewaySharedWithFootAndBicycle()).getPermission()
    );
  }

  @Test
  void testMotorway() {
    assertEquals(CAR, wps.getDataForWay(WayTestData.motorway()).forward().getPermission());
    assertEquals(NONE, wps.getDataForWay(WayTestData.motorway()).backward().getPermission());
  }

  @Test
  void testMotorwayWithBicycleAllowed() {
    assertEquals(
      BICYCLE_AND_CAR,
      wps.getDataForWay(WayTestData.motorwayWithBicycleAllowed()).forward().getPermission()
    );
    assertEquals(
      NONE,
      wps.getDataForWay(WayTestData.motorwayWithBicycleAllowed()).backward().getPermission()
    );
  }

  @Test
  void testPrimaryMotorroad() {
    assertEquals(
      CAR,
      wps.getDataForEntity(WayTestData.highwayPrimaryWithMotorroad()).getPermission()
    );
  }

  @Test
  void testTrunk() {
    assertEquals(ALL, wps.getDataForEntity(WayTestData.highwayTrunk()).getPermission());
  }

  @Test
  void testTrunkMotorroad() {
    assertEquals(
      CAR,
      wps.getDataForEntity(WayTestData.highwayTrunkWithMotorroad()).getPermission()
    );
  }

  @Test
  void testTrunkWalkSafety() {
    var rawScore = wps.getDataForWay(WayTestData.highwayTrunk()).forward().walkSafety();
    var scoreWithLane = wps
      .getDataForWay((OsmWay) WayTestData.highwayTrunk().addTag("sidewalk", "lane"))
      .forward()
      .walkSafety();
    var scoreWithSidewalk = wps
      .getDataForWay((OsmWay) WayTestData.highwayTrunk().addTag("sidewalk", "both"))
      .forward()
      .walkSafety();
    var scoreWithSeparateSidewalk = wps
      .getDataForWay((OsmWay) WayTestData.highwayTrunk().addTag("sidewalk", "separate"))
      .forward()
      .walkSafety();
    assertTrue(rawScore > 5);
    assertTrue(scoreWithLane < rawScore);
    assertTrue(scoreWithSidewalk < scoreWithLane);
    assertEquals(rawScore, scoreWithSeparateSidewalk);
  }

  @Test
  void testTertiary() {
    assertEquals(ALL, wps.getDataForEntity(WayTestData.highwayTertiary()).getPermission());
  }

  @Test
  void testFootModifier() {
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("foot", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testVehicleDenied() {
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testVehicleDeniedMotorVehiclePermissive() {
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("motor_vehicle", "designated");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testVehicleDeniedBicyclePermissive() {
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("bicycle", "designated");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testMotorcycleModifier() {
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("motor_vehicle", "yes");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testBicycleModifier() {
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testBicyclePermissive() {
    OsmEntity tags = new OsmEntity();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "permissive");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  void testOneWay() {
    var way = WayTestData.highwayTertiary();
    way.addTag("oneway", "yes");
    var props = wps.getDataForWay(way);
    assertEquals(ALL, props.forward().getPermission());
    assertEquals(PEDESTRIAN, props.backward().getPermission());

    way.addTag("oneway:bicycle", "no");
    props = wps.getDataForWay(way);
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
    final WayPropertySet wps = wayProperySet();

    assertEquals(ALL, wps.getDataForWay(way).forward().getPermission());

    way.addTag("motorroad", "yes");
    assertEquals(CAR, wps.getDataForWay(way).forward().getPermission());
  }

  @Test
  void corridorName() {
    final WayPropertySet wps = wayProperySet();
    var way = way("highway", "corridor");
    assertEquals("corridor", wps.getCreativeNameForWay(way).toString());
    assertEquals("Korridor", wps.getCreativeNameForWay(way).toString(Locale.GERMANY));
    assertEquals("käytävä", wps.getCreativeNameForWay(way).toString(FI));
  }

  @Test
  void indoorAreaName() {
    var wps = wayProperySet();
    var way = way("indoor", "area");
    assertEquals("indoor area", wps.getCreativeNameForWay(way).toString());
    assertEquals("Innenbereich", wps.getCreativeNameForWay(way).toString(Locale.GERMANY));
    assertEquals("sisätila", wps.getCreativeNameForWay(way).toString(FI));
  }

  public OsmEntity way(String key, String value) {
    var way = new OsmEntity();
    way.addTag(key, value);
    return way;
  }

  private static WayPropertySet wayProperySet() {
    OsmTagMapper osmTagMapper = new OsmTagMapper();
    WayPropertySet wps = new WayPropertySet();
    osmTagMapper.populateProperties(wps);
    return wps;
  }
}
