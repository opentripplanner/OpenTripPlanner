package org.opentripplanner.openstreetmap.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData;

public class OSMWayTest {

  @Test
  void testIsBicycleDismountForced() {
    OSMWay way = new OSMWay();
    assertFalse(way.isBicycleDismountForced());

    way.addTag("bicycle", "dismount");
    assertTrue(way.isBicycleDismountForced());
  }

  @Test
  void testAreaMustContain3Nodes() {
    OSMWay way = new OSMWay();
    way.addTag("area", "yes");
    assertFalse(way.isArea());
    way.addNodeRef(1);
    assertFalse(way.isArea());
    way.addNodeRef(2);
    assertFalse(way.isArea());
    way.addNodeRef(3);
    assertTrue(way.isArea());
    way.addNodeRef(4);
    assertTrue(way.isArea());
  }

  @Test
  void testAreaTags() {
    OSMWay platform = getClosedPolygon();
    platform.addTag("public_transport", "platform");
    assertTrue(platform.isArea());
    platform.addTag("area", "no");
    assertFalse(platform.isArea());

    OSMWay roundabout = getClosedPolygon();
    roundabout.addTag("highway", "roundabout");
    assertFalse(roundabout.isArea());

    OSMWay pedestrian = getClosedPolygon();
    pedestrian.addTag("highway", "pedestrian");
    assertFalse(pedestrian.isArea());
    pedestrian.addTag("area", "yes");
    assertTrue(pedestrian.isArea());

    OSMWay indoorArea = getClosedPolygon();
    indoorArea.addTag("indoor", "area");
    assertTrue(indoorArea.isArea());

    OSMWay bikeParking = getClosedPolygon();
    bikeParking.addTag("amenity", "bicycle_parking");
    assertTrue(bikeParking.isArea());

    OSMWay corridor = getClosedPolygon();
    corridor.addTag("indoor", "corridor");
    assertTrue(corridor.isArea());

    OSMWay door = getClosedPolygon();
    door.addTag("indoor", "door");
    assertFalse(door.isArea());
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

  private OSMWay getClosedPolygon() {
    var way = new OSMWay();
    way.addNodeRef(1);
    way.addNodeRef(2);
    way.addNodeRef(3);
    way.addNodeRef(1);
    return way;
  }
}
