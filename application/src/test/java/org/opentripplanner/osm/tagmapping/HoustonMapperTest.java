package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.NONE;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.model.OsmEntityForTest;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

class HoustonMapperTest {

  static final WayPropertySet WPS = new HoustonMapper().buildWayPropertySet();

  @Test
  public void lamarTunnel() {
    // https://www.openstreetmap.org/way/127288293
    OsmEntity tunnel = new OsmEntityForTest();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("indoor", "yes");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("name", "Lamar Tunnel");
    tunnel.addTag("tunnel", "yes");

    assertEquals(NONE, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void harrisCountyTunnel() {
    // https://www.openstreetmap.org/way/127288288
    OsmEntity tunnel = new OsmEntityForTest();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("indoor", "yes");
    tunnel.addTag("name", "Harris County Tunnel");
    tunnel.addTag("tunnel", "yes");

    assertEquals(PEDESTRIAN, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void pedestrianUnderpass() {
    // https://www.openstreetmap.org/way/783648925
    OsmEntity tunnel = new OsmEntityForTest();
    tunnel.addTag("highway", "footway");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("tunnel", "yes");

    assertEquals(PEDESTRIAN, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void cyclingTunnel() {
    // https://www.openstreetmap.org/way/220484967
    OsmEntity tunnel = new OsmEntityForTest();
    tunnel.addTag("bicycle", "designated");
    tunnel.addTag("foot", "designated");
    tunnel.addTag("highway", "cycleway");
    tunnel.addTag("segregated", "no");
    tunnel.addTag("surface", "concrete");
    tunnel.addTag("tunnel", "yes");

    assertEquals(PEDESTRIAN_AND_BICYCLE, WPS.getDataForEntity(tunnel).getPermission());

    // https://www.openstreetmap.org/way/101884176
    tunnel = new OsmEntityForTest();
    tunnel.addTag("highway", "cycleway");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("name", "Hogg Woods Trail");
    tunnel.addTag("tunnel", "yes");
    assertEquals(PEDESTRIAN_AND_BICYCLE, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void carTunnel() {
    // https://www.openstreetmap.org/way/598694756
    var tunnel = new OsmWay();
    tunnel.addTag("highway", "primary");
    tunnel.addTag("hov", "lane");
    tunnel.addTag("lanes", "4");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("lit", "yes");
    tunnel.addTag("maxspeed", "30 mph");
    tunnel.addTag("nam", "San Jacinto Street");
    tunnel.addTag("note:lanes", "right lane is hov");
    tunnel.addTag("oneway", "yes");
    tunnel.addTag("surface", "concrete");
    tunnel.addTag("tunnel", "yes");

    assertEquals(ALL, WPS.getDataForWay(tunnel).forward().getPermission());
  }

  @Test
  public void carUnderpass() {
    // https://www.openstreetmap.org/way/102925214
    var tunnel = new OsmWay();
    tunnel.addTag("highway", "motorway_link");
    tunnel.addTag("lanes", "2");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("oneway", "yes");
    tunnel.addTag("tunnel", "yes");

    assertEquals(CAR, WPS.getDataForWay(tunnel).forward().getPermission());
  }

  @Test
  public void serviceTunnel() {
    // https://www.openstreetmap.org/way/15334550
    OsmEntity tunnel = new OsmEntityForTest();
    tunnel.addTag("highway", "service");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("tunnel", "yes");

    assertEquals(ALL, WPS.getDataForEntity(tunnel).getPermission());
  }

  @Test
  public void unclassified() {
    // https://www.openstreetmap.org/way/44896136
    OsmEntity tunnel = new OsmEntityForTest();
    tunnel.addTag("highway", "unclassified");
    tunnel.addTag("name", "Ross Sterling Street");
    tunnel.addTag("layer", "-1");
    tunnel.addTag("tunnel", "yes");

    assertEquals(ALL, WPS.getDataForEntity(tunnel).getPermission());
  }
}
