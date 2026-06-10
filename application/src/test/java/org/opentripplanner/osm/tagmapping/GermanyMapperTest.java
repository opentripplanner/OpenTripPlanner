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

  static final WayPropertySet WPS = new GermanyMapper().buildWayPropertySet();
  static float epsilon = 0.01f;

  /**
   * Test that bike safety factors are calculated accurately
   */
  @Nested
  class BikeSafety {

    @Test
    void testBikeSafety() {
      // way 361961158
      var way = OsmWay.of()
        .withTag("bicycle", "yes")
        .withTag("foot", "designated")
        .withTag("footway", "sidewalk")
        .withTag("highway", "footway")
        .withTag("lit", "yes")
        .withTag("oneway", "no")
        .withTag("traffic_sign", "DE:239,1022-10")
        .build();
      assertEquals(1.2, WPS.getDataForWay(way).forward().bicycleSafety(), epsilon);
    }

    @Test
    void cyclewayOpposite() {
      var way = OsmWay.of()
        .withTag("cycleway", "opposite")
        .withTag("highway", "residential")
        .withTag("lit", "yes")
        .withTag("maxspeed", "30")
        .withTag("name", "Freibadstraße")
        .withTag("oneway", "yes")
        .withTag("oneway:bicycle", "no")
        .withTag("parking:lane:left", "parallel")
        .withTag("parking:lane:right", "no_parking")
        .withTag("sidewalk", "both")
        .withTag("source:maxspeed", "DE:zone:30")
        .withTag("surface", "asphalt")
        .withTag("width", "6.5")
        .withTag("zone:traffic", "DE:urban")
        .build();
      assertEquals(0.9, WPS.getDataForWay(way).forward().bicycleSafety(), epsilon);
      // walk safety should be default
      assertEquals(1, WPS.getDataForWay(way).forward().walkSafety(), epsilon);
    }

    @Test
    void bikePath() {
      // way332589799 (Radschnellweg BW1)
      var way = OsmWay.of()
        .withTag("bicycle", "designated")
        .withTag("class:bicycle", "2")
        .withTag("class:bicycle:roadcycling", "1")
        .withTag("highway", "track")
        .withTag("horse", "forestry")
        .withTag("lcn", "yes")
        .withTag("lit", "yes")
        .withTag("maxspeed", "30")
        .withTag("motor_vehicle", "forestry")
        .withTag("name", "Römerstraße")
        .withTag("smoothness", "excellent")
        .withTag("source:maxspeed", "sign")
        .withTag("surface", "asphalt")
        .withTag("tracktype", "grade1")
        .build();
      assertEquals(0.693, WPS.getDataForWay(way).forward().bicycleSafety(), epsilon);
    }

    @Test
    void track() {
      var way = OsmWay.of()
        .withTag("highway", "track")
        .withTag("motor_vehicle", "agricultural")
        .withTag("surface", "asphalt")
        .withTag("tracktype", "grade1")
        .withTag("traffic_sign", "DE:260,1026-36")
        .withTag("width", "2.5")
        .build();
      assertEquals(1.0, WPS.getDataForWay(way).forward().bicycleSafety(), epsilon);
    }
  }

  @Test
  void testPermissions() {
    // https://www.openstreetmap.org/way/124263424
    var way = OsmWay.of().withTag("highway", "track").withTag("tracktype", "grade1").build();
    assertEquals(
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(way).getPermission()
    );

    // https://www.openstreetmap.org/way/5155805
    way = OsmWay.of()
      .withTag("access:lanes:forward", "yes|no")
      .withTag("bicycle:lanes:forward", "|designated")
      .withTag("change:lanes:forward", "not_right|no")
      .withTag("cycleway:right", "lane")
      .withTag("cycleway:right:lane", "exclusive")
      .withTag("cycleway:right:traffic_sign", "DE:237")
      .withTag("highway", "unclassified")
      .withTag("lanes", "3")
      .withTag("lanes:backward", "2")
      .withTag("lanes:forward", "1")
      .withTag("lit", "yes")
      .withTag("maxspeed", "50")
      .withTag("name", "Krailenshaldenstraße")
      .withTag("parking:lane:both", "no_stopping")
      .withTag("sidewalk", "left")
      .withTag("smoothness", "good")
      .withTag("source:maxspeed", "DE:urban")
      .withTag("surface", "asphalt")
      .withTag("turn:lanes:backward", "left|through;right")
      .withTag("width:lanes:forward", "|1.4")
      .withTag("zone:traffic", "DE:urban")
      .build();

    assertEquals(ALL, WPS.getDataForEntity(way).getPermission());
  }

  @Nested
  class BikeRouteNetworks {

    @Test
    void lcnAndRcnShouldNotBeAddedUp() {
      // https://www.openstreetmap.org/way/26443041 is part of both an lcn and rcn but that shouldn't mean that
      // it is to be more heavily favoured than other ways that are part of just one.

      var both = OsmWay.of()
        .withTag("highway", "residential")
        .withTag("rcn", "yes")
        .withTag("lcn", "yes")
        .build();

      var justLcn = OsmWay.of().withTag("lcn", "yes").withTag("highway", "residential").build();

      var residential = OsmWay.of().withTag("highway", "residential").build();

      assertEquals(
        WPS.getDataForWay(both).forward().bicycleSafety(),
        WPS.getDataForWay(justLcn).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(0.6859, WPS.getDataForWay(both).forward().bicycleSafety(), epsilon);

      assertEquals(0.98, WPS.getDataForWay(residential).forward().bicycleSafety(), epsilon);
    }

    @Test
    void bicycleRoadAndLcnShouldNotBeAddedUp() {
      // https://www.openstreetmap.org/way/22201321 was tagged as bicycle_road without lcn
      // make it so all ways tagged as some kind of cyclestreets are considered as equally safe

      var both = OsmWay.of()
        .withTag("highway", "residential")
        .withTag("bicycle_road", "yes")
        .withTag("cyclestreet", "yes")
        .withTag("lcn", "yes")
        .build();

      var justBicycleRoad = OsmWay.of()
        .withTag("bicycle_road", "yes")
        .withTag("highway", "residential")
        .build();

      var justCyclestreet = OsmWay.of()
        .withTag("cyclestreet", "yes")
        .withTag("highway", "residential")
        .build();

      var justLcn = OsmWay.of().withTag("lcn", "yes").withTag("highway", "residential").build();

      var residential = OsmWay.of().withTag("highway", "residential").build();

      assertEquals(
        WPS.getDataForWay(justCyclestreet).forward().bicycleSafety(),
        WPS.getDataForWay(justLcn).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(
        WPS.getDataForWay(both).forward().bicycleSafety(),
        WPS.getDataForWay(justBicycleRoad).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(
        WPS.getDataForWay(both).forward().bicycleSafety(),
        WPS.getDataForWay(justCyclestreet).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(
        WPS.getDataForWay(both).forward().bicycleSafety(),
        WPS.getDataForWay(justLcn).forward().bicycleSafety(),
        epsilon
      );

      assertEquals(0.6859, WPS.getDataForWay(both).forward().bicycleSafety(), epsilon);

      assertEquals(0.98, WPS.getDataForWay(residential).forward().bicycleSafety(), epsilon);
    }
  }

  @Test
  void setCorrectPermissionsForRoundabouts() {
    // https://www.openstreetmap.org/way/184185551
    var residential = OsmWay.of()
      .withTag("highway", "residential")
      .withTag("junction", "roundabout")
      .build();
    assertEquals(ALL, WPS.getDataForWay(residential).forward().getPermission());
    assertEquals(PEDESTRIAN, WPS.getDataForWay(residential).backward().getPermission());

    //https://www.openstreetmap.org/way/31109939
    var primary = OsmWay.of()
      .withTag("highway", "primary")
      .withTag("junction", "roundabout")
      .build();
    assertEquals(BICYCLE_AND_CAR, WPS.getDataForWay(primary).forward().getPermission());
    assertEquals(NONE, WPS.getDataForWay(primary).backward().getPermission());
  }

  @Test
  void setCorrectBikeSafetyValuesForBothDirections() {
    // https://www.openstreetmap.org/way/13420871
    var residential = OsmWay.of()
      .withTag("highway", "residential")
      .withTag("lit", "yes")
      .withTag("maxspeed", "30")
      .withTag("name", "Auf der Heide")
      .withTag("surface", "asphalt")
      .build();
    assertEquals(
      WPS.getDataForWay(residential).forward().bicycleSafety(),
      WPS.getDataForWay(residential).backward().bicycleSafety(),
      epsilon
    );
  }

  @Test
  void setCorrectPermissionsForSteps() {
    // https://www.openstreetmap.org/way/64359102
    var steps = OsmWay.of().withTag("highway", "steps").build();
    assertEquals(StreetTraversalPermission.PEDESTRIAN, WPS.getDataForEntity(steps).getPermission());
  }

  @Test
  void testGermanAutobahnSpeed() {
    // https://www.openstreetmap.org/way/10879847
    var alzentalstr = OsmWay.of()
      .withTag("highway", "residential")
      .withTag("lit", "yes")
      .withTag("maxspeed", "30")
      .withTag("name", "Alzentalstraße")
      .withTag("surface", "asphalt")
      .build();
    assertEquals(
      8.33333969116211,
      WPS.getCarSpeedForWay(alzentalstr, TraverseDirection.FORWARD),
      epsilon
    );

    var autobahn = OsmWay.of().withTag("highway", "motorway").withTag("maxspeed", "none").build();
    assertEquals(
      33.33000183105469,
      WPS.getCarSpeedForWay(autobahn, TraverseDirection.FORWARD),
      epsilon
    );
  }

  /**
   * Test that biking is not allowed in transit platforms
   */
  @Test
  public void testArea() {
    var way = OsmWay.of().withTag("public_transport", "platform").withTag("area", "yes").build();
    assertEquals(PEDESTRIAN, WPS.getDataForEntity(way).getPermission());
    way = way.copy().withTag("bicycle", "yes").build();
    assertEquals(PEDESTRIAN_AND_BICYCLE, WPS.getDataForEntity(way).getPermission());
  }
}
