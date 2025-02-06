package org.opentripplanner.osm.wayproperty.specifier;

import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.model.OsmWithTags;

public class WayTestData {

  public static OsmWithTags carTunnel() {
    // https://www.openstreetmap.org/way/598694756
    OsmWithTags tunnel = new OsmWithTags();
    tunnel.addTag("highway", "primary");
    tunnel.addTag("hov", "lane");
    tunnel.addTag("lanes", "4");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("maxspeed", "30 mph");
    tunnel.addTag("name", "San Jacinto Street");
    tunnel.addTag("note:lanes", "right lane is hov");
    tunnel.addTag("oneway", "yes");
    tunnel.addTag("surface", "concrete");
    tunnel.addTag("tunnel", "yes");
    return tunnel;
  }

  public static OsmWithTags pedestrianTunnel() {
    // https://www.openstreetmap.org/way/127288293
    OsmWithTags tunnel = new OsmWithTags();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("indoor", "yes");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("name", "Lamar Tunnel");
    tunnel.addTag("tunnel", "yes");
    return tunnel;
  }

  public static OsmWithTags streetOnBikeRoute() {
    // https://www.openstreetmap.org/way/26443041 is part of both an lcn relation

    var way = new OsmWithTags();
    way.addTag("highway", "residential");
    way.addTag("lit", "yes");
    way.addTag("maxspeed", "30");
    way.addTag("name", "Schulstraße");
    way.addTag("oneway", "no");
    way.addTag("surface", "sett");
    way.addTag("rcn", "yes");
    way.addTag("lcn", "yes");

    return way;
  }

  public static OsmWithTags stairs() {
    // https://www.openstreetmap.org/way/1058669389
    var way = new OsmWithTags();
    way.addTag("handrail", "yes");
    way.addTag("highway", "steps");
    way.addTag("incline", "down");
    way.addTag("ramp", "yes");
    way.addTag("ramp:bicycle", "yes");
    way.addTag("oneway", "no");
    way.addTag("step_count", "38");
    way.addTag("surface", "metal");

    return way;
  }

  public static OsmWithTags southeastLaBonitaWay() {
    // https://www.openstreetmap.org/way/5302874
    var way = new OsmWithTags();
    way.addTag("highway", "residential");
    way.addTag("name", "Southeast la Bonita Way");
    way.addTag("sidewalk", "both");

    return way;
  }

  public static OsmWithTags southwestMayoStreet() {
    //https://www.openstreetmap.org/way/425004690
    var way = new OsmWithTags();
    way.addTag("highway", "residential");
    way.addTag("name", "Southwest Mayo Street");
    way.addTag("maxspeed", "25 mph");
    way.addTag("sidewalk", "left");

    return way;
  }

  public static OsmWithTags fiveLanes() {
    OsmWithTags way = new OsmWithTags();
    way.addTag("highway", "primary");
    way.addTag("lanes", "5");
    return way;
  }

  public static OsmWithTags threeLanes() {
    OsmWithTags way = new OsmWithTags();
    way.addTag("highway", "primary");
    way.addTag("lanes", "3");
    return way;
  }

  public static OsmWay cycleway() {
    var way = new OsmWay();
    way.addTag("highway", "residential");
    way.addTag("cycleway", "lane");
    return way;
  }

  public static OsmWithTags cyclewayLeft() {
    var way = new OsmWithTags();
    way.addTag("highway", "residential");
    way.addTag("cycleway:left", "lane");
    return way;
  }

  public static OsmWithTags cyclewayBoth() {
    var way = new OsmWithTags();
    way.addTag("highway", "residential");
    way.addTag("cycleway:both", "lane");
    return way;
  }

  public static OsmWay footwaySidewalk() {
    var way = new OsmWay();
    way.addTag("footway", "sidewalk");
    way.addTag("highway", "footway");
    return way;
  }

  public static OsmWithTags sidewalkBoth() {
    var way = new OsmWithTags();
    way.addTag("highway", "both");
    way.addTag("sidewalk", "both");
    return way;
  }

  public static OsmWithTags noSidewalk() {
    var way = new OsmWithTags();
    way.addTag("highway", "residential");
    way.addTag("sidewalk", "no");
    return way;
  }

  public static OsmWithTags noSidewalkHighSpeed() {
    var way = new OsmWithTags();
    way.addTag("highway", "residential");
    way.addTag("sidewalk", "no");
    way.addTag("maxspeed", "55 mph");
    return way;
  }

  public static OsmWithTags highwayTrunk() {
    var way = new OsmWithTags();
    way.addTag("highway", "trunk");
    return way;
  }

  public static OsmWay highwayTertiary() {
    var way = new OsmWay();
    way.addTag("highway", "tertiary");
    return way;
  }

  public static OsmWithTags highwayTertiaryWithSidewalk() {
    var way = new OsmWithTags();
    way.addTag("highway", "tertiary");
    way.addTag("sidewalk", "both");
    return way;
  }

  public static OsmWithTags cobblestones() {
    var way = new OsmWithTags();
    way.addTag("highway", "residential");
    way.addTag("surface", "cobblestones");
    return way;
  }

  public static OsmWithTags cyclewayLaneTrack() {
    var way = new OsmWithTags();
    way.addTag("highway", "footway");
    way.addTag("cycleway", "lane");
    way.addTag("cycleway:right", "track");
    return way;
  }

  public static OsmWithTags tramsForward() {
    // https://www.openstreetmap.org/way/108037345
    var way = new OsmWithTags();
    way.addTag("highway", "tertiary");
    way.addTag("embedded_rails:forward", "tram");
    return way;
  }

  public static OsmWithTags veryBadSmoothness() {
    // https://www.openstreetmap.org/way/11402648
    var way = new OsmWithTags();
    way.addTag("highway", "footway");
    way.addTag("surface", "sett");
    way.addTag("smoothness", "very_bad");
    return way;
  }

  public static OsmWithTags excellentSmoothness() {
    // https://www.openstreetmap.org/way/437167371
    var way = new OsmWithTags();
    way.addTag("highway", "cycleway");
    way.addTag("segregated", "no");
    way.addTag("surface", "asphalt");
    way.addTag("smoothness", "excellent");
    return way;
  }

  public static OsmWithTags zooPlatform() {
    // https://www.openstreetmap.org/way/119108622
    var way = new OsmWithTags();
    way.addTag("public_transport", "platform");
    way.addTag("usage", "tourism");
    return way;
  }

  public static OsmWithTags indoor(String value) {
    var way = new OsmWithTags();
    way.addTag("indoor", value);
    return way;
  }

  public static OsmWithTags parkAndRide() {
    var way = new OsmWithTags();
    way.addTag("amenity", "parking");
    way.addTag("park_ride", "yes");
    way.addTag("capacity", "10");
    return way;
  }

  public static OsmWay platform() {
    var way = new OsmWay();
    way.addTag("public_transport", "platform");
    way.addTag("ref", "123");
    return way;
  }
}
