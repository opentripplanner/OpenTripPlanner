package org.opentripplanner.osm.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.osm.model.TraverseDirection.BACKWARD;
import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;

import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

class OsmWayTest {

  @Test
  void testIsBicycleDismountForced() {
    OsmWay way = new OsmWay();
    assertFalse(way.isBicycleDismountForced());

    way.addTag("bicycle", "dismount");
    assertTrue(way.isBicycleDismountForced());
  }

  @Test
  void testAreaMustContain3Nodes() {
    OsmWay way = new OsmWay();
    way.addTag("area", "yes");
    assertFalse(way.isRoutableArea());
    way.addNodeRef(1);
    assertFalse(way.isRoutableArea());
    way.addNodeRef(2);
    assertFalse(way.isRoutableArea());
    way.addNodeRef(3);
    assertTrue(way.isRoutableArea());
    way.addNodeRef(4);
    assertTrue(way.isRoutableArea());
  }

  @Test
  void testAreaTags() {
    OsmWay platform = getClosedPolygon();
    platform.addTag("public_transport", "platform");
    assertTrue(platform.isRoutableArea());
    platform.addTag("area", "no");
    assertFalse(platform.isRoutableArea());

    OsmWay roundabout = getClosedPolygon();
    roundabout.addTag("highway", "roundabout");
    assertFalse(roundabout.isRoutableArea());

    OsmWay pedestrian = getClosedPolygon();
    pedestrian.addTag("highway", "pedestrian");
    assertFalse(pedestrian.isRoutableArea());
    pedestrian.addTag("area", "yes");
    assertTrue(pedestrian.isRoutableArea());

    OsmWay indoorArea = getClosedPolygon();
    indoorArea.addTag("indoor", "area");
    assertTrue(indoorArea.isRoutableArea());

    OsmWay bikeParking = getClosedPolygon();
    bikeParking.addTag("amenity", "bicycle_parking");
    assertTrue(bikeParking.isRoutableArea());

    OsmWay corridor = getClosedPolygon();
    corridor.addTag("indoor", "corridor");
    assertTrue(corridor.isRoutableArea());

    OsmWay door = getClosedPolygon();
    door.addTag("indoor", "door");
    assertFalse(door.isRoutableArea());
  }

  @Test
  void testIsSteps() {
    OsmWay way = new OsmWay();
    assertFalse(way.isSteps());

    way.addTag("highway", "primary");
    assertFalse(way.isSteps());

    way.addTag("highway", "steps");
    assertTrue(way.isSteps());
  }

  @Test
  void testIsStairs() {
    OsmWay way = new OsmWay();
    assertFalse(way.isStairs());

    way.addTag("highway", "primary");
    assertFalse(way.isStairs());

    way.addTag("highway", "steps");
    assertTrue(way.isStairs());

    way.addTag("conveying", "yes");
    assertFalse(way.isStairs());
  }

  @Test
  void wheelchairAccessibleStairs() {
    var osm1 = new OsmWay();
    osm1.addTag("highway", "steps");
    assertFalse(osm1.isWheelchairAccessible());

    // explicitly suitable for wheelchair users, perhaps because of a ramp
    var osm2 = new OsmWay();
    osm2.addTag("highway", "steps");
    osm2.addTag("wheelchair", "yes");
    assertTrue(osm2.isWheelchairAccessible());
  }

  @Test
  void testIsRoundabout() {
    OsmWay way = new OsmWay();
    assertFalse(way.isRoundabout());

    way.addTag("junction", "dovetail");
    assertFalse(way.isRoundabout());

    way.addTag("junction", "roundabout");
    assertTrue(way.isRoundabout());
  }

  @Test
  void testIsOneWayDriving() {
    assertEquals(Optional.empty(), new OsmWay().isOneWay("motorcar"));
    assertEquals(
      Optional.empty(),
      new OsmWay().addTag("oneway", "notatagvalue").isOneWay("motorcar")
    );
    assertEquals(Optional.empty(), new OsmWay().addTag("oneway", "no").isOneWay("motorcar"));
    assertEquals(Optional.of(FORWARD), new OsmWay().addTag("oneway", "1").isOneWay("motorcar"));
    assertEquals(Optional.of(FORWARD), new OsmWay().addTag("oneway", "true").isOneWay("motorcar"));
    assertEquals(Optional.of(BACKWARD), new OsmWay().addTag("oneway", "-1").isOneWay("motorcar"));
    assertEquals(
      Optional.of(FORWARD),
      new OsmWay().addTag("junction", "roundabout").isOneWay("motorcar")
    );
    assertEquals(
      Optional.of(FORWARD),
      new OsmWay().addTag("highway", "motorway").isOneWay("motorcar")
    );
  }

  @Test
  void testIsOneWayBicycle() {
    assertEquals(Optional.empty(), new OsmWay().isOneWay("bicycle"));
    assertEquals(
      Optional.empty(),
      new OsmWay().addTag("oneway", "notatagvalue").isOneWay("bicycle")
    );
    assertEquals(Optional.empty(), new OsmWay().addTag("oneway", "no").isOneWay("bicycle"));
    assertEquals(Optional.of(FORWARD), new OsmWay().addTag("oneway", "1").isOneWay("bicycle"));
    assertEquals(Optional.of(FORWARD), new OsmWay().addTag("oneway", "true").isOneWay("bicycle"));
    assertEquals(Optional.of(BACKWARD), new OsmWay().addTag("oneway", "-1").isOneWay("bicycle"));
    assertEquals(
      Optional.of(FORWARD),
      new OsmWay().addTag("junction", "roundabout").isOneWay("bicycle")
    );

    assertEquals(
      Optional.empty(),
      new OsmWay().addTag("oneway", "yes").addTag("oneway:bicycle", "no").isOneWay("bicycle")
    );
    assertEquals(
      Optional.of(FORWARD),
      new OsmWay().addTag("oneway", "no").addTag("oneway:bicycle", "yes").isOneWay("bicycle")
    );
    assertEquals(
      Optional.empty(),
      new OsmWay().addTag("oneway", "yes").addTag("bicycle:backward", "yes").isOneWay("bicycle")
    );
    assertEquals(
      Optional.empty(),
      new OsmWay().addTag("oneway", "yes").addTag("cycleway", "opposite").isOneWay("bicycle")
    );
    assertEquals(
      Optional.empty(),
      new OsmWay().addTag("oneway", "yes").addTag("cycleway", "opposite_lane").isOneWay("bicycle")
    );
  }

  @Test
  void testIsOneWayFoot() {
    assertEquals(Optional.empty(), new OsmWay().isOneWay("foot"));
    assertEquals(Optional.empty(), new OsmWay().addTag("oneway", "notatagvalue").isOneWay("foot"));
    assertEquals(Optional.empty(), new OsmWay().addTag("oneway", "no").isOneWay("foot"));
    assertEquals(Optional.empty(), new OsmWay().addTag("oneway", "1").isOneWay("foot"));
    assertEquals(Optional.empty(), new OsmWay().addTag("oneway", "true").isOneWay("foot"));
    assertEquals(Optional.empty(), new OsmWay().addTag("oneway", "-1").isOneWay("foot"));
    assertEquals(Optional.empty(), new OsmWay().addTag("junction", "roundabout").isOneWay("foot"));

    assertEquals(Optional.of(FORWARD), new OsmWay().addTag("oneway:foot", "yes").isOneWay("foot"));
    assertEquals(Optional.of(BACKWARD), new OsmWay().addTag("oneway:foot", "-1").isOneWay("foot"));
    assertEquals(
      Optional.of(FORWARD),
      new OsmWay().addTag("highway", "footway").addTag("oneway", "yes").isOneWay("foot")
    );
    assertEquals(
      Optional.of(BACKWARD),
      new OsmWay().addTag("highway", "footway").addTag("oneway", "-1").isOneWay("foot")
    );
  }

  @Test
  void testIsOpposableCycleway() {
    OsmWay way = new OsmWay();
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

  @Test
  void testIsEscalator() {
    assertFalse(WayTestData.highwayWithCycleLane().isEscalator());

    var escalator = new OsmWay();
    escalator.addTag("highway", "steps");
    assertFalse(escalator.isEscalator());

    escalator.addTag("conveying", "yes");
    assertTrue(escalator.isEscalator());

    escalator.addTag("conveying", "whoknows?");
    assertFalse(escalator.isEscalator());

    escalator.addTag("conveying", "forward");
    assertTrue(escalator.isForwardEscalator());

    escalator.addTag("conveying", "backward");
    assertTrue(escalator.isBackwardEscalator());
  }

  @Test
  void isRelevantForRouting() {
    var way = new OsmWay();
    way.addTag("highway", "residential");
    assertTrue(way.isRelevantForRouting());
    way.addTag("access", "no");
    assertFalse(way.isRelevantForRouting());

    way = new OsmWay();
    way.addTag("amenity", "parking");
    way.addTag("area", "yes");
    assertFalse(way.isRelevantForRouting());
    way.addTag("park_ride", "train");
    assertTrue(way.isRelevantForRouting());

    way = new OsmWay();
    way.addTag("amenity", "bicycle_parking");
    way.addTag("area", "yes");
    assertTrue(way.isRelevantForRouting());

    way = new OsmWay();
    way.addTag("public_transport", "platform");
    way.addTag("area", "yes");
    assertTrue(way.isRelevantForRouting());
  }

  private OsmWay getClosedPolygon() {
    var way = new OsmWay();
    way.addNodeRef(1);
    way.addNodeRef(2);
    way.addNodeRef(3);
    way.addNodeRef(1);
    return way;
  }

  private static OsmWay createCrossing(String crossingTag, String crossingValue) {
    var way = WayTestData.footway();
    way.addTag("footway", "crossing");
    way.addTag(crossingTag, crossingValue);
    return way;
  }

  @Test
  void footway() {
    assertFalse(WayTestData.highwayPrimary().isFootway());
    assertTrue(WayTestData.footway().isFootway());
  }

  @Test
  void serviceRoad() {
    assertFalse(WayTestData.highwayPrimary().isServiceRoad());

    var way = new OsmWay();
    way.addTag("highway", "service");
    assertTrue(way.isServiceRoad());
  }

  @Test
  void motorwayRamp() {
    assertFalse(WayTestData.highwayPrimary().isMotorwayRamp());
    assertFalse(WayTestData.motorway().isMotorwayRamp());
    assertTrue(WayTestData.motorwayRamp().isMotorwayRamp());
  }

  @Test
  void turnLane() {
    assertFalse(WayTestData.highwayTertiary().isTurnLane());

    var namedOneWay = new OsmWay();
    namedOneWay.addTag("name", "3rd Street");
    namedOneWay.addTag("oneway", "yes");
    assertFalse(namedOneWay.isTurnLane());

    var oneWay = WayTestData.highwayTertiary();
    oneWay.addTag("oneway", "yes");
    assertTrue(oneWay.isTurnLane());
  }

  @ParameterizedTest
  @MethodSource("createRampAsTurnLaneCases")
  void rampAsTurnLane(String turnValue, boolean oneWay, boolean expected) {
    var ramp = WayTestData.motorwayRamp();
    if (oneWay) {
      ramp.addTag("oneway", "yes");
    }
    ramp.addTag("turn:lanes", turnValue);

    assertEquals(
      expected,
      ramp.isTurnLane(),
      String.format(
        "%s-way ramp with '%s' turn lane attribute %s a turn lane.",
        oneWay ? "One" : "Two",
        turnValue,
        expected ? "should be" : "should not be"
      )
    );
  }

  static Stream<Arguments> createRampAsTurnLaneCases() {
    return Stream.of(
      Arguments.of("right", true, true),
      Arguments.of("right", false, false),
      Arguments.of("left", true, true),
      Arguments.of("left", false, false),
      Arguments.of("merge_left", true, false),
      Arguments.of("merge_left", false, false),
      Arguments.of(null, true, false),
      Arguments.of(null, false, false)
    );
  }

  @ParameterizedTest
  @MethodSource("createCrossingCases")
  void crossing(OsmWay way, boolean result) {
    assertEquals(result, way.isCrossing());
  }

  static Stream<Arguments> createCrossingCases() {
    return Stream.of(
      Arguments.of(WayTestData.footway(), false),
      Arguments.of(WayTestData.footwaySidewalk(), false),
      Arguments.of(createCrossing("crossing", "marked"), true),
      Arguments.of(createCrossing("crossing", "other"), true),
      Arguments.of(createCrossing("crossing:markings", "yes"), true),
      Arguments.of(createCrossing("crossing:markings", "marking-details"), true),
      Arguments.of(createCrossing("crossing:markings", null), true),
      Arguments.of(createCrossing("crossing:markings", "no"), true)
    );
  }

  @Test
  void adjacentTo() {
    final long nodeId1 = 10001L;
    final long nodeId2 = 20002L;
    final long sharedNodeId = 30003L;

    OsmWay way1 = new OsmWay();
    OsmWay way2 = new OsmWay();
    assertFalse(way1.isAdjacentTo(way2));

    var nodes1 = way1.getNodeRefs();
    var nodes2 = way2.getNodeRefs();
    nodes1.add(sharedNodeId);
    nodes1.add(nodeId1);
    nodes2.add(nodeId2);
    assertFalse(way1.isAdjacentTo(way2));

    nodes2.add(sharedNodeId);
    assertTrue(way1.isAdjacentTo(way2));
  }
}
