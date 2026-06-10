package org.opentripplanner.osm.wayproperty.specifier;

import org.opentripplanner.osm.model.OsmWay;

public class WayTestData {

  public static OsmWay carTunnel() {
    // https://www.openstreetmap.org/way/598694756
    return OsmWay.of()
      .withTag("highway", "primary")
      .withTag("hov", "lane")
      .withTag("lanes", "4")
      .withTag("layer", "-1")
      .withTag("lit", "yes")
      .withTag("maxspeed", "30 mph")
      .withTag("name", "San Jacinto Street")
      .withTag("note:lanes", "right lane is hov")
      .withTag("oneway", "yes")
      .withTag("surface", "concrete")
      .withTag("tunnel", "yes")
      .build();
  }

  public static OsmWay pedestrianTunnel() {
    // https://www.openstreetmap.org/way/127288293
    return OsmWay.of()
      .withTag("highway", "footway")
      .withTag("indoor", "yes")
      .withTag("layer", "-1")
      .withTag("lit", "yes")
      .withTag("name", "Lamar Tunnel")
      .withTag("tunnel", "yes")
      .build();
  }

  public static OsmWay streetOnBikeRoute() {
    // https://www.openstreetmap.org/way/26443041 is part of both an lcn relation
    return OsmWay.of()
      .withTag("highway", "residential")
      .withTag("lit", "yes")
      .withTag("maxspeed", "30")
      .withTag("name", "Schulstraße")
      .withTag("oneway", "no")
      .withTag("surface", "sett")
      .withTag("rcn", "yes")
      .withTag("lcn", "yes")
      .build();
  }

  public static OsmWay stairs() {
    // https://www.openstreetmap.org/way/1058669389
    return OsmWay.of()
      .withTag("handrail", "yes")
      .withTag("highway", "steps")
      .withTag("incline", "down")
      .withTag("ramp", "yes")
      .withTag("ramp:bicycle", "yes")
      .withTag("oneway", "no")
      .withTag("step_count", "38")
      .withTag("surface", "metal")
      .build();
  }

  public static OsmWay southeastLaBonitaWay() {
    // https://www.openstreetmap.org/way/5302874
    return OsmWay.of()
      .withTag("highway", "residential")
      .withTag("name", "Southeast la Bonita Way")
      .withTag("sidewalk", "both")
      .build();
  }

  public static OsmWay southwestMayoStreet() {
    //https://www.openstreetmap.org/way/425004690
    return OsmWay.of()
      .withTag("highway", "residential")
      .withTag("name", "Southwest Mayo Street")
      .withTag("maxspeed", "25 mph")
      .withTag("sidewalk", "left")
      .build();
  }

  public static OsmWay fiveLanes() {
    return OsmWay.of().withTag("highway", "primary").withTag("lanes", "5").build();
  }

  public static OsmWay threeLanes() {
    return OsmWay.of().withTag("highway", "primary").withTag("lanes", "3").build();
  }

  public static OsmWay highwayWithCycleLane() {
    return OsmWay.of().withTag("highway", "residential").withTag("cycleway", "lane").build();
  }

  public static OsmWay cyclewayLeft() {
    return OsmWay.of().withTag("highway", "residential").withTag("cycleway:left", "lane").build();
  }

  public static OsmWay cyclewayBoth() {
    return OsmWay.of().withTag("highway", "residential").withTag("cycleway:both", "lane").build();
  }

  public static OsmWay footway() {
    return OsmWay.of().withTag("highway", "footway").build();
  }

  public static OsmWay footwaySharedWithBicycle() {
    return OsmWay.of()
      .withTag("highway", "footway")
      .withTag("foot", "designated")
      .withTag("bicycle", "designated")
      .build();
  }

  public static OsmWay cycleway() {
    return OsmWay.of().withTag("highway", "cycleway").build();
  }

  public static OsmWay cyclewaySharedWithFoot() {
    return OsmWay.of()
      .withTag("highway", "cycleway")
      .withTag("foot", "designated")
      .withTag("bicycle", "designated")
      .build();
  }

  public static OsmWay footwaySidewalk() {
    return OsmWay.of().withTag("footway", "sidewalk").withTag("highway", "footway").build();
  }

  public static OsmWay bridleway() {
    return OsmWay.of().withTag("highway", "bridleway").build();
  }

  public static OsmWay bridlewaySharedWithFootAndBicycle() {
    return OsmWay.of()
      .withTag("highway", "bridleway")
      .withTag("foot", "designated")
      .withTag("bicycle", "designated")
      .build();
  }

  public static OsmWay pedestrianArea() {
    return OsmWay.of().withTag("area", "yes").withTag("highway", "pedestrian").build();
  }

  public static OsmWay sidewalkBoth() {
    return OsmWay.of().withTag("highway", "primary").withTag("sidewalk", "both").build();
  }

  public static OsmWay noSidewalk() {
    return OsmWay.of().withTag("highway", "residential").withTag("sidewalk", "no").build();
  }

  public static OsmWay noSidewalkHighSpeed() {
    return OsmWay.of()
      .withTag("highway", "residential")
      .withTag("sidewalk", "no")
      .withTag("maxspeed", "55 mph")
      .build();
  }

  public static OsmWay path() {
    return OsmWay.of().withTag("highway", "path").build();
  }

  public static OsmWay motorway() {
    return OsmWay.of().withTag("highway", "motorway").build();
  }

  public static OsmWay motorwayWithBicycleAllowed() {
    return OsmWay.of().withTag("highway", "motorway").withTag("bicycle", "yes").build();
  }

  public static OsmWay motorwayRamp() {
    return OsmWay.of().withTag("highway", "motorway_link").build();
  }

  public static OsmWay highwayTrunk() {
    return OsmWay.of().withTag("highway", "trunk").build();
  }

  public static OsmWay highwayTrunkWithMotorroad() {
    return OsmWay.of().withTag("highway", "trunk").withTag("motorroad", "yes").build();
  }

  public static OsmWay highwayPrimary() {
    return OsmWay.of().withTag("highway", "primary").build();
  }

  public static OsmWay highwayPrimaryWithMotorroad() {
    return highwayPrimary().copy().withTag("motorroad", "yes").build();
  }

  public static OsmWay highwayTertiary() {
    return OsmWay.of().withTag("highway", "tertiary").build();
  }

  public static OsmWay highwaySecondary() {
    return OsmWay.of().withTag("highway", "secondary").build();
  }

  public static OsmWay highwayService() {
    return OsmWay.of().withTag("highway", "service").build();
  }

  public static OsmWay highwayServiceWithSidewalk() {
    return highwayService().copy().withTag("sidewalk", "both").build();
  }

  public static OsmWay highwayPedestrian() {
    return OsmWay.of().withTag("highway", "pedestrian").build();
  }

  public static OsmWay highwayPedestrianWithSidewalk() {
    return highwayPedestrian().copy().withTag("sidewalk", "both").build();
  }

  public static OsmWay highwayTertiaryWithSidewalk() {
    return OsmWay.of().withTag("highway", "tertiary").withTag("sidewalk", "both").build();
  }

  public static OsmWay cobblestones() {
    return OsmWay.of().withTag("highway", "residential").withTag("surface", "cobblestones").build();
  }

  public static OsmWay cyclewayLaneTrack() {
    return OsmWay.of()
      .withTag("highway", "footway")
      .withTag("cycleway", "lane")
      .withTag("cycleway:right", "track")
      .build();
  }

  public static OsmWay tramsForward() {
    // https://www.openstreetmap.org/way/108037345
    return OsmWay.of()
      .withTag("highway", "tertiary")
      .withTag("embedded_rails:forward", "tram")
      .build();
  }

  public static OsmWay veryBadSmoothness() {
    // https://www.openstreetmap.org/way/11402648
    return OsmWay.of()
      .withTag("highway", "footway")
      .withTag("surface", "sett")
      .withTag("smoothness", "very_bad")
      .build();
  }

  public static OsmWay excellentSmoothness() {
    // https://www.openstreetmap.org/way/437167371
    return OsmWay.of()
      .withTag("highway", "cycleway")
      .withTag("segregated", "no")
      .withTag("surface", "asphalt")
      .withTag("smoothness", "excellent")
      .build();
  }

  public static OsmWay zooPlatform() {
    // https://www.openstreetmap.org/way/119108622
    return OsmWay.of().withTag("public_transport", "platform").withTag("usage", "tourism").build();
  }

  public static OsmWay indoor(String value) {
    return OsmWay.of().withTag("indoor", value).build();
  }

  public static OsmWay parkAndRide() {
    return OsmWay.of()
      .withTag("amenity", "parking")
      .withTag("park_ride", "yes")
      .withTag("capacity", "10")
      .build();
  }

  public static OsmWay platform() {
    return OsmWay.of().withTag("public_transport", "platform").withTag("ref", "123").build();
  }
}
