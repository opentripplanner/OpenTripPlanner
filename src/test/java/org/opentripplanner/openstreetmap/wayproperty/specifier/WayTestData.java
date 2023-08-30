package org.opentripplanner.openstreetmap.wayproperty.specifier;

import org.opentripplanner.openstreetmap.model.OSMWay;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class WayTestData {

  public static OSMWithTags carTunnel() {
    // https://www.openstreetmap.org/way/598694756
    OSMWithTags tunnel = new OSMWithTags();
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

  public static OSMWithTags pedestrianTunnel() {
    // https://www.openstreetmap.org/way/127288293
    OSMWithTags tunnel = new OSMWithTags();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("indoor", "yes");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("name", "Lamar Tunnel");
    tunnel.addTag("tunnel", "yes");
    return tunnel;
  }

  public static OSMWithTags streetOnBikeRoute() {
    // https://www.openstreetmap.org/way/26443041 is part of both an lcn relation

    var way = new OSMWithTags();
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

  public static OSMWithTags stairs() {
    // https://www.openstreetmap.org/way/1058669389
    var way = new OSMWithTags();
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

  public static OSMWithTags southeastLaBonitaWay() {
    // https://www.openstreetmap.org/way/5302874
    var way = new OSMWithTags();
    way.addTag("highway", "residential");
    way.addTag("name", "Southeast la Bonita Way");
    way.addTag("sidewalk", "both");

    return way;
  }

  public static OSMWithTags southwestMayoStreet() {
    //https://www.openstreetmap.org/way/425004690
    var way = new OSMWithTags();
    way.addTag("highway", "residential");
    way.addTag("name", "Southwest Mayo Street");
    way.addTag("maxspeed", "25 mph");
    way.addTag("sidewalk", "left");

    return way;
  }

  public static OSMWithTags fiveLanes() {
    OSMWithTags way = new OSMWithTags();
    way.addTag("highway", "primary");
    way.addTag("lanes", "5");
    return way;
  }

  public static OSMWithTags threeLanes() {
    OSMWithTags way = new OSMWithTags();
    way.addTag("highway", "primary");
    way.addTag("lanes", "3");
    return way;
  }

  public static OSMWay cycleway() {
    var way = new OSMWay();
    way.addTag("highway", "residential");
    way.addTag("cycleway", "lane");
    return way;
  }

  public static OSMWithTags cyclewayLeft() {
    var way = new OSMWithTags();
    way.addTag("highway", "residential");
    way.addTag("cycleway:left", "lane");
    return way;
  }

  public static OSMWithTags cyclewayBoth() {
    var way = new OSMWithTags();
    way.addTag("highway", "residential");
    way.addTag("cycleway:both", "lane");
    return way;
  }

  public static OSMWithTags footwaySidewalk() {
    var way = new OSMWithTags();
    way.addTag("footway", "sidewalk");
    return way;
  }

  public static OSMWithTags sidewalkBoth() {
    var way = new OSMWithTags();
    way.addTag("highway", "both");
    way.addTag("sidewalk", "both");
    return way;
  }

  public static OSMWithTags noSidewalk() {
    var way = new OSMWithTags();
    way.addTag("highway", "residential");
    way.addTag("sidewalk", "no");
    return way;
  }

  public static OSMWithTags noSidewalkHighSpeed() {
    var way = new OSMWithTags();
    way.addTag("highway", "residential");
    way.addTag("sidewalk", "no");
    way.addTag("maxspeed", "55 mph");
    return way;
  }

  public static OSMWithTags highwayTrunk() {
    var way = new OSMWithTags();
    way.addTag("highway", "trunk");
    return way;
  }

  public static OSMWithTags highwayTertiary() {
    var way = new OSMWithTags();
    way.addTag("highway", "tertiary");
    return way;
  }

  public static OSMWithTags highwayTertiaryWithSidewalk() {
    var way = new OSMWithTags();
    way.addTag("highway", "tertiary");
    way.addTag("sidewalk", "both");
    return way;
  }

  public static OSMWithTags cobblestones() {
    var way = new OSMWithTags();
    way.addTag("highway", "residential");
    way.addTag("surface", "cobblestones");
    return way;
  }

  public static OSMWithTags cyclewayLaneTrack() {
    var way = new OSMWithTags();
    way.addTag("highway", "footway");
    way.addTag("cycleway", "lane");
    way.addTag("cycleway:right", "track");
    return way;
  }

  public static OSMWithTags tramsForward() {
    // https://www.openstreetmap.org/way/108037345
    var way = new OSMWithTags();
    way.addTag("highway", "tertiary");
    way.addTag("embedded_rails:forward", "tram");
    return way;
  }

  public static OSMWithTags veryBadSmoothness() {
    // https://www.openstreetmap.org/way/11402648
    var way = new OSMWithTags();
    way.addTag("highway", "footway");
    way.addTag("surface", "sett");
    way.addTag("smoothness", "very_bad");
    return way;
  }

  public static OSMWithTags excellentSmoothness() {
    // https://www.openstreetmap.org/way/437167371
    var way = new OSMWithTags();
    way.addTag("highway", "cycleway");
    way.addTag("segregated", "no");
    way.addTag("surface", "asphalt");
    way.addTag("smoothness", "excellent");
    return way;
  }

  public static OSMWithTags zooPlatform() {
    // https://www.openstreetmap.org/way/119108622
    var way = new OSMWithTags();
    way.addTag("public_transport", "platform");
    way.addTag("usage", "tourism");
    return way;
  }
}
