package org.opentripplanner.openstreetmap.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
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
  @Test
  public void testBikeSafety() {
    OSMWithTags way;

    // way 361961158
    way = new OSMWithTags();
    way.addTag("bicycle", "yes");
    way.addTag("foot", "designated");
    way.addTag("footway", "sidewalk");
    way.addTag("highway", "footway");
    way.addTag("lit", "yes");
    way.addTag("oneway", "no");
    way.addTag("traffic_sign", "DE:239,1022-10");
    assertEquals(1.2, wps.getDataForWay(way).getBicycleSafetyFeatures().forward(), epsilon);

    way = new OSMWithTags();
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
    assertEquals(0.9, wps.getDataForWay(way).getBicycleSafetyFeatures().forward(), epsilon);
    // walk safety should be default
    assertEquals(1, wps.getDataForWay(way).getWalkSafetyFeatures().forward(), epsilon);

    // way332589799 (Radschnellweg BW1)
    way = new OSMWithTags();
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
    assertEquals(0.693, wps.getDataForWay(way).getBicycleSafetyFeatures().forward(), epsilon);

    way = new OSMWithTags();
    way.addTag("highway", "track");
    way.addTag("motor_vehicle", "agricultural");
    way.addTag("surface", "asphalt");
    way.addTag("tracktype", "grade1");
    way.addTag("traffic_sign", "DE:260,1026-36");
    way.addTag("width", "2.5");
    assertEquals(1.0, wps.getDataForWay(way).getBicycleSafetyFeatures().forward(), epsilon);
  }

  @Test
  public void testPermissions() {
    // https://www.openstreetmap.org/way/124263424
    var way = new OSMWithTags();
    way.addTag("highway", "track");
    way.addTag("tracktype", "grade1");
    assertEquals(
      wps.getDataForWay(way).getPermission(),
      StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE
    );

    // https://www.openstreetmap.org/way/5155805
    way = new OSMWithTags();
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

    assertEquals(wps.getDataForWay(way).getPermission(), StreetTraversalPermission.ALL);
  }

  @Test
  public void lcnAndRcnShouldNotBeAddedUp() {
    // https://www.openstreetmap.org/way/26443041 is part of both an lcn and rnc but that shouldn't mean that
    // it is to be more heavily favoured than other ways that are part of just one.

    var both = new OSMWithTags();
    both.addTag("highway", "residential");
    both.addTag("rcn", "yes");
    both.addTag("lcn", "yes");

    var justLcn = new OSMWithTags();
    justLcn.addTag("lcn", "yes");
    justLcn.addTag("highway", "residential");

    var residential = new OSMWithTags();
    residential.addTag("highway", "residential");

    assertEquals(
      wps.getDataForWay(both).getBicycleSafetyFeatures().forward(),
      wps.getDataForWay(justLcn).getBicycleSafetyFeatures().forward(),
      epsilon
    );

    assertEquals(wps.getDataForWay(both).getBicycleSafetyFeatures().forward(), 0.6859, epsilon);

    assertEquals(
      wps.getDataForWay(residential).getBicycleSafetyFeatures().forward(),
      0.98,
      epsilon
    );
  }

  @Test
  public void setCorrectPermissionsForRoundabouts() {
    // https://www.openstreetmap.org/way/184185551
    var residential = new OSMWithTags();
    residential.addTag("highway", "residential");
    residential.addTag("junction", "roundabout");
    assertEquals(wps.getDataForWay(residential).getPermission(), StreetTraversalPermission.ALL);

    //https://www.openstreetmap.org/way/31109939
    var primary = new OSMWithTags();
    primary.addTag("highway", "primary");
    primary.addTag("junction", "roundabout");
    assertEquals(
      wps.getDataForWay(primary).getPermission(),
      StreetTraversalPermission.BICYCLE_AND_CAR
    );
  }

  @Test
  public void setCorrectBikeSafetyValuesForBothDirections() {
    // https://www.openstreetmap.org/way/13420871
    var residential = new OSMWithTags();
    residential.addTag("highway", "residential");
    residential.addTag("lit", "yes");
    residential.addTag("maxspeed", "30");
    residential.addTag("name", "Auf der Heide");
    residential.addTag("surface", "asphalt");
    assertEquals(
      wps.getDataForWay(residential).getBicycleSafetyFeatures().forward(),
      wps.getDataForWay(residential).getBicycleSafetyFeatures().back(),
      epsilon
    );
  }

  @Test
  public void setCorrectPermissionsForSteps() {
    // https://www.openstreetmap.org/way/64359102
    var steps = new OSMWithTags();
    steps.addTag("highway", "steps");
    assertEquals(wps.getDataForWay(steps).getPermission(), StreetTraversalPermission.PEDESTRIAN);
  }

  @Test
  public void testGermanAutobahnSpeed() {
    // https://www.openstreetmap.org/way/10879847
    var alzentalstr = new OSMWithTags();
    alzentalstr.addTag("highway", "residential");
    alzentalstr.addTag("lit", "yes");
    alzentalstr.addTag("maxspeed", "30");
    alzentalstr.addTag("name", "Alzentalstraße");
    alzentalstr.addTag("surface", "asphalt");
    assertEquals(8.33333969116211, wps.getCarSpeedForWay(alzentalstr, false), epsilon);

    var autobahn = new OSMWithTags();
    autobahn.addTag("highway", "motorway");
    autobahn.addTag("maxspeed", "none");
    assertEquals(33.33000183105469, wps.getCarSpeedForWay(autobahn, false), epsilon);
  }
}
