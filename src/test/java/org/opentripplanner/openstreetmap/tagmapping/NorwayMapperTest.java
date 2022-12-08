package org.opentripplanner.openstreetmap.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

public class NorwayMapperTest {

  static WayPropertySet wps = new WayPropertySet();

  static {
    var source = new NorwayMapper();
    source.populateProperties(wps);
  }

  @Test
  public void testMtbScaleNone() {
    // https://www.openstreetmap.org/way/302610220
    var way1 = new OSMWithTags();
    way1.addTag("highway", "path");
    way1.addTag("mtb:scale", "3");

    assertEquals(StreetTraversalPermission.NONE, wps.getDataForWay(way1).getPermission());

    var way2 = new OSMWithTags();
    way2.addTag("highway", "track");
    way2.addTag("mtb:scale", "3");

    assertEquals(StreetTraversalPermission.NONE, wps.getDataForWay(way2).getPermission());
  }

  @Test
  public void testMtbScalePedestrian() {
    var way1 = new OSMWithTags();
    way1.addTag("highway", "path");
    way1.addTag("mtb:scale", "1");

    assertEquals(StreetTraversalPermission.PEDESTRIAN, wps.getDataForWay(way1).getPermission());

    var way2 = new OSMWithTags();
    way2.addTag("highway", "track");
    way2.addTag("mtb:scale", "1");

    assertEquals(StreetTraversalPermission.PEDESTRIAN, wps.getDataForWay(way2).getPermission());
  }

  @Test
  public void testMotorroad() {
    var way1 = new OSMWithTags();
    way1.addTag("highway", "trunk");
    way1.addTag("motorroad", "yes");

    assertEquals(StreetTraversalPermission.CAR, wps.getDataForWay(way1).getPermission());

    var way2 = new OSMWithTags();
    way2.addTag("highway", "primary");
    way2.addTag("motorroad", "yes");

    assertEquals(StreetTraversalPermission.CAR, wps.getDataForWay(way2).getPermission());
  }
}
