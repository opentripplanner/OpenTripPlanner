package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

class AtlantaMapperTest {

  static WayPropertySet wps = new WayPropertySet();

  static {
    AtlantaMapper source = new AtlantaMapper();
    source.populateProperties(wps);
  }

  // Most OSM trunk roads in Atlanta are (large) city roads that are permitted for all modes.
  // (The default TagMapper implementation is car-only.)
  // TODO: Handle exceptions such as:
  //  - Northside Drive between Marietta Street and Tech Parkway (northbound)
  //    (https://www.openstreetmap.org/way/96395009, no sidewalk, but possible to bike)
  //  - Portions of Freedom Parkway that are freeway/motorway-like (https://www.openstreetmap.org/way/88171817)

  @Test
  public void peachtreeRoad() {
    // Peachtree Rd in Atlanta has sidewalks, and bikes are allowed.
    // https://www.openstreetmap.org/way/144429544
    OsmEntity peachtreeRd = new OsmEntity();
    peachtreeRd.addTag("highway", "trunk");
    peachtreeRd.addTag("lanes", "6");
    peachtreeRd.addTag("name", "Peachtree Road");
    peachtreeRd.addTag("ref", "US 19;GA 9");
    peachtreeRd.addTag("surface", "asphalt");
    peachtreeRd.addTag("tiger:county", "Fulton, GA");

    assertEquals(StreetTraversalPermission.ALL, wps.getDataForWay(peachtreeRd).getPermission());
  }

  @Test
  public void deKalbAvenue() {
    // "Outer" ramps from DeKalb Ave onto Moreland Ave in Atlanta have sidewalks, and bikes are allowed.
    // https://www.openstreetmap.org/way/9164434
    OsmEntity morelandRamp = new OsmEntity();
    morelandRamp.addTag("highway", "trunk_link");
    morelandRamp.addTag("lanes", "1");
    morelandRamp.addTag("oneway", "yes");
    morelandRamp.addTag("tiger:cfcc", "A63");
    morelandRamp.addTag("tiger:county", "DeKalb, GA");
    morelandRamp.addTag("tiger:reviewed", "no");

    assertEquals(StreetTraversalPermission.ALL, wps.getDataForWay(morelandRamp).getPermission());
  }

  @Test
  public void tenthStreetNE() {
    // For sanity check, secondary roads (e.g. 10th Street) should remain allowed for all modes.
    // https://www.openstreetmap.org/way/505912700
    OsmEntity tenthSt = new OsmEntity();
    tenthSt.addTag("highway", "secondary");
    tenthSt.addTag("lanes", "4");
    tenthSt.addTag("maxspeed", "30 mph");
    tenthSt.addTag("name", "10th Street Northeast");
    tenthSt.addTag("oneway", "no");
    tenthSt.addTag("source:maxspeed", "sign");
    tenthSt.addTag("surface", "asphalt");
    tenthSt.addTag("tiger:cfcc", "A41");
    tenthSt.addTag("tiger:county", "Fulton, GA");
    tenthSt.addTag("tiger:reviewed", "no");
    // Some other params omitted.
    assertEquals(StreetTraversalPermission.ALL, wps.getDataForWay(tenthSt).getPermission());
  }
}
