package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.BICYCLE_AND_CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.TraverseDirection;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class GermanyMapperTest {

  static WayPropertySet wps = new WayPropertySet();
  static float epsilon = 0.01f;

  static {
    var source = new GermanyMapper();
    source.populateProperties(wps);
  }

  /**
   * Test that bike safety factors are calculated accurately
   */
  @Nested
  class BikeSafety {

    @Test
    void testBikeSafety() {
      // way 361961158
      var way = new OsmWay();
      way.addTag("bicycle", "yes");
      way.addTag("foot", "designated");
      way.addTag("footway", "sidewalk");
      way.addTag("highway", "footway");
      way.addTag("lit", "yes");
      way.addTag("oneway", "no");
      way.addTag("traffic_sign", "DE:239,1022-10");
      assertEquals(1.2, wps.getDataForWay(way).forward().bicycleSafety(), epsilon);
    }

    @Test
    void cyclewayOpposite() {
      var way = new OsmWay();
      way.addTag("cycleway", "opposite");
      way.addTag("highway", "residential");
      way.addTag("lit", "yes");
      way.addTag("maxspeed", "30");
      way.addTag("name", "Freibadstraße");
      way.addTag("oneway", "yes");
      way.addTag("oneway:bicycle", "no");
      way.addTag("parking:lane:left", "parallel");
      way.addTag("parking:lane:right", "no_parking");
      way.addTag("sidewalk", "both");
      way.addTag("source:maxspeed", "DE:zone:30");
      way.addTag("surface", "asphalt");
      way.addTag("width", "6.5");
      way.addTag("zone:traffic", "DE:urban");
      assertEquals(0.9, wps.getDataForWay(way).forward().bicycleSafety(), epsilon);
      // walk safety should be default
      assertEquals(1, wps.getDataForWay(way).forward().walkSafety(), epsilon);
    }

    @Test
    void bikePath() {
      // way332589799 (Radschnellweg BW1)
      var way = new OsmWay();
      way.addTag("bicycle", "designated");
      way.addTag("class:bicycle", "2");
      way.addTag("class:bicycle:roadcycling", "1");
      way.addTag("highway", "track");
      way.addTag("horse", "forestry");
      way.addTag("lcn", "yes");
      way.addTag("lit", "yes");
      way.addTag("maxspeed", "30");
      way.addTag("motor_vehicle", "forestry");
      way.addTag("name", "Römerstraße");
      way.addTag("smoothness", "excellent");
      way.addTag("source:maxspeed", "sign");
      way.addTag("surface", "asphalt");
      way.addTag("tracktype", "grade1");
      assertEquals(0.693, wps.getDataForWay(way).forward().bicycleSafety(), epsilon);
    }

    @Test
    void track() {
      var way = new OsmWay();
      way.addTag("highway", "track");
      way.addTag("motor_vehicle", "agricultural");
      way.addTag("surface", "asphalt");
      way.addTag("tracktype", "grade1");
      way.addTag("traffic_sign", "DE:260,1026-36");
      way.addTag("width", "2.5");
      assertEquals(1.0, wps.getDataForWay(way).forward().bicycleSafety(), epsilon);
    }
  }

  @Test
  void testPermissions() {
    // https://www.openstreetmap.org/way/124263424
    var way = new OsmWay();
    way.addTag("highway", "track");
    way.addTag("tracktype", "grade1");
    assertEquals(
      wps.getDataForEntity(way).getPermission(),
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
    );

    // https://www.openstreetmap.org/way/5155805
    way = new OsmWay();
    way.addTag("access:lanes:forward", "yes|no");
    way.addTag("bicycle:lanes:forward", "|designated");
    way.addTag("change:lanes:forward", "not_right|no");
    way.addTag("cycleway:right", "lane");
    way.addTag("cycleway:right:lane", "exclusive");
    way.addTag("cycleway:right:traffic_sign", "DE:237");
    way.addTag("highway", "unclassified");
    way.addTag("lanes", "3");
    way.addTag("lanes:backward", "2");
    way.addTag("lanes:forward", "1");
    way.addTag("lit", "yes");
    way.addTag("maxspeed", "50");
    way.addTag("name", "Krailenshaldenstraße");
    way.addTag("parking:lane:both", "no_stopping");
    way.addTag("sidewalk", "left");
    way.addTag("smoothness", "good");
    way.addTag("source:maxspeed", "DE:urban");
    way.addTag("surface", "asphalt");
    way.addTag("turn:lanes:backward", "left|through;right");
    way.addTag("width:lanes:forward", "|1.4");
    way.addTag("zone:traffic", "DE:urban");

    assertEquals(ALL, wps.getDataForEntity(way).getPermission());
  }

  @Nested
  class BikeRouteNetworks {

    @Test
    void lcnAndRcnShouldNotBeAddedUp() {
      // https://www.openstreetmap.org/way/26443041 is part of both an lcn and rcn but that shouldn't mean that
      // it is to be more heavily favoured than other ways that are part of just one.

      var both = new OsmWay();
      both.addTag("highway", "residential");
      both.addTag("rcn", "yes");
      both.addTag("lcn", "yes");

      var justLcn = new OsmWay();
      justLcn.addTag("lcn", "yes");
      justLcn.addTag("highway", "residential");

      var residential = new OsmWay();
      residential.addTag("highway", "residential");

      assertEquals(
        wps.getDataForWay(both).forward().bicycleSafety(),
        wps.getDataForWay(justLcn).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(0.6859, wps.getDataForWay(both).forward().bicycleSafety(), epsilon);

      assertEquals(0.98, wps.getDataForWay(residential).forward().bicycleSafety(), epsilon);
    }

    @Test
    void bicycleRoadAndLcnShouldNotBeAddedUp() {
      // https://www.openstreetmap.org/way/22201321 was tagged as bicycle_road without lcn
      // make it so all ways tagged as some kind of cyclestreets are considered as equally safe

      var both = new OsmWay();
      both.addTag("highway", "residential");
      both.addTag("bicycle_road", "yes");
      both.addTag("cyclestreet", "yes");
      both.addTag("lcn", "yes");

      var justBicycleRoad = new OsmWay();
      justBicycleRoad.addTag("bicycle_road", "yes");
      justBicycleRoad.addTag("highway", "residential");

      var justCyclestreet = new OsmWay();
      justCyclestreet.addTag("cyclestreet", "yes");
      justCyclestreet.addTag("highway", "residential");

      var justLcn = new OsmWay();
      justLcn.addTag("lcn", "yes");
      justLcn.addTag("highway", "residential");

      var residential = new OsmWay();
      residential.addTag("highway", "residential");

      assertEquals(
        wps.getDataForWay(justCyclestreet).forward().bicycleSafety(),
        wps.getDataForWay(justLcn).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(
        wps.getDataForWay(both).forward().bicycleSafety(),
        wps.getDataForWay(justBicycleRoad).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(
        wps.getDataForWay(both).forward().bicycleSafety(),
        wps.getDataForWay(justCyclestreet).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(
        wps.getDataForWay(both).forward().bicycleSafety(),
        wps.getDataForWay(justLcn).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(0.6859, wps.getDataForWay(both).forward().bicycleSafety(), epsilon);

      assertEquals(0.98, wps.getDataForWay(residential).forward().bicycleSafety(), epsilon);
    }
  }

  @Test
  void setCorrectPermissionsForRoundabouts() {
    // https://www.openstreetmap.org/way/184185551
    var residential = new OsmWay();
    residential.addTag("highway", "residential");
    residential.addTag("junction", "roundabout");
    assertEquals(ALL, wps.getDataForWay(residential).forward().getPermission());
    assertEquals(PEDESTRIAN, wps.getDataForWay(residential).backward().getPermission());

    //https://www.openstreetmap.org/way/31109939
    var primary = new OsmWay();
    primary.addTag("highway", "primary");
    primary.addTag("junction", "roundabout");
    assertEquals(BICYCLE_AND_CAR, wps.getDataForWay(primary).forward().getPermission());
    assertEquals(NONE, wps.getDataForWay(primary).backward().getPermission());
  }

  @Test
  void setCorrectBikeSafetyValuesForBothDirections() {
    // https://www.openstreetmap.org/way/13420871
    var residential = new OsmWay();
    residential.addTag("highway", "residential");
    residential.addTag("lit", "yes");
    residential.addTag("maxspeed", "30");
    residential.addTag("name", "Auf der Heide");
    residential.addTag("surface", "asphalt");
    assertEquals(
      wps.getDataForWay(residential).forward().bicycleSafety(),
      wps.getDataForWay(residential).backward().bicycleSafety(),
      epsilon
    );
  }

  @Test
  void setCorrectPermissionsForSteps() {
    // https://www.openstreetmap.org/way/64359102
    var steps = new OsmWay();
    steps.addTag("highway", "steps");
    assertEquals(StreetTraversalPermission.PEDESTRIAN, wps.getDataForEntity(steps).getPermission());
  }

  @Test
  void testGermanAutobahnSpeed() {
    // https://www.openstreetmap.org/way/10879847
    var alzentalstr = new OsmWay();
    alzentalstr.addTag("highway", "residential");
    alzentalstr.addTag("lit", "yes");
    alzentalstr.addTag("maxspeed", "30");
    alzentalstr.addTag("name", "Alzentalstraße");
    alzentalstr.addTag("surface", "asphalt");
    assertEquals(
      8.33333969116211,
      wps.getCarSpeedForWay(alzentalstr, TraverseDirection.FORWARD),
      epsilon
    );

    var autobahn = new OsmWay();
    autobahn.addTag("highway", "motorway");
    autobahn.addTag("maxspeed", "none");
    assertEquals(
      33.33000183105469,
      wps.getCarSpeedForWay(autobahn, TraverseDirection.FORWARD),
      epsilon
    );
  }

  /**
   * Test that biking is not allowed in transit platforms
   */
  @Test
  public void testArea() {
    var way = new OsmWay();
    way.addTag("public_transport", "platform");
    way.addTag("area", "yes");
    assertEquals(PEDESTRIAN, wps.getDataForEntity(way).getPermission());
    way.addTag("bicycle", "yes");
    assertEquals(PEDESTRIAN_AND_BICYCLE, wps.getDataForEntity(way).getPermission());
  }
}
