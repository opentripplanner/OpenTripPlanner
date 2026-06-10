package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

class HoustonMapperTest {

  static final WayPropertySet WPS = new HoustonMapper().buildWayPropertySet();

  @Test
  public void lamarTunnel() {
    // https://www.openstreetmap.org/way/127288293
    var tunnel = OsmWay.of()
      .withTag("highway", "footway")
      .withTag("indoor", "yes")
      .withTag("layer", "-1")
      .withTag("lit", "yes")
      .withTag("name", "Lamar Tunnel")
      .withTag("tunnel", "yes")
      .build();

    assertEquals(NONE, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void harrisCountyTunnel() {
    // https://www.openstreetmap.org/way/127288288
    var tunnel = OsmWay.of()
      .withTag("highway", "footway")
      .withTag("indoor", "yes")
      .withTag("name", "Harris County Tunnel")
      .withTag("tunnel", "yes")
      .build();

    assertEquals(PEDESTRIAN, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void pedestrianUnderpass() {
    // https://www.openstreetmap.org/way/783648925
    var tunnel = OsmWay.of()
      .withTag("highway", "footway")
      .withTag("layer", "-1")
      .withTag("tunnel", "yes")
      .build();

    assertEquals(PEDESTRIAN, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void cyclingTunnel() {
    // https://www.openstreetmap.org/way/220484967
    var tunnel = OsmWay.of()
      .withTag("bicycle", "designated")
      .withTag("foot", "designated")
      .withTag("highway", "cycleway")
      .withTag("segregated", "no")
      .withTag("surface", "concrete")
      .withTag("tunnel", "yes")
      .build();

    assertEquals(PEDESTRIAN_AND_BICYCLE, WPS.getDataForEntity(tunnel).getPermission());

    // https://www.openstreetmap.org/way/101884176
    tunnel = OsmWay.of()
      .withTag("highway", "cycleway")
      .withTag("layer", "-1")
      .withTag("name", "Hogg Woods Trail")
      .withTag("tunnel", "yes")
      .build();
    assertEquals(PEDESTRIAN_AND_BICYCLE, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void carTunnel() {
    // https://www.openstreetmap.org/way/598694756
    var tunnel = OsmWay.of()
      .withTag("highway", "primary")
      .withTag("hov", "lane")
      .withTag("lanes", "4")
      .withTag("layer", "-1")
      .withTag("lit", "yes")
      .withTag("maxspeed", "30 mph")
      .withTag("nam", "San Jacinto Street")
      .withTag("note:lanes", "right lane is hov")
      .withTag("oneway", "yes")
      .withTag("surface", "concrete")
      .withTag("tunnel", "yes")
      .build();

    assertEquals(ALL, WPS.getDataForWay(tunnel).forward().getPermission());
  }

  @Test
  public void carUnderpass() {
    // https://www.openstreetmap.org/way/102925214
    var tunnel = OsmWay.of()
      .withTag("highway", "motorway_link")
      .withTag("lanes", "2")
      .withTag("layer", "-1")
      .withTag("oneway", "yes")
      .withTag("tunnel", "yes")
      .build();

    assertEquals(CAR, WPS.getDataForWay(tunnel).forward().getPermission());
  }

  @Test
  public void serviceTunnel() {
    // https://www.openstreetmap.org/way/15334550
    var tunnel = OsmWay.of()
      .withTag("highway", "service")
      .withTag("layer", "-1")
      .withTag("tunnel", "yes")
      .build();

    assertEquals(ALL, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void unclassified() {
    // https://www.openstreetmap.org/way/44896136
    var tunnel = OsmWay.of()
      .withTag("highway", "unclassified")
      .withTag("name", "Ross Sterling Street")
      .withTag("layer", "-1")
      .withTag("tunnel", "yes")
      .build();

    assertEquals(ALL, WPS.getDataForEntity(tunnel).getPermission());
  }
}
