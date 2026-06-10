package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

class AtlantaMapperTest {

  private static final WayPropertySet WPS = new AtlantaMapper().buildWayPropertySet();

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
    OsmWay peachtreeRd = OsmWay.of()
      .withTag("highway", "trunk")
      .withTag("lanes", "6")
      .withTag("name", "Peachtree Road")
      .withTag("ref", "US 19;GA 9")
      .withTag("surface", "asphalt")
      .withTag("tiger:county", "Fulton, GA")
      .build();

    assertEquals(StreetTraversalPermission.ALL, WPS.getDataForEntity(peachtreeRd).getPermission());
  }

  @Test
  public void deKalbAvenue() {
    // "Outer" ramps from DeKalb Ave onto Moreland Ave in Atlanta have sidewalks, and bikes are allowed.
    // https://www.openstreetmap.org/way/9164434
    OsmWay morelandRamp = OsmWay.of()
      .withTag("highway", "trunk_link")
      .withTag("lanes", "1")
      .withTag("oneway", "yes")
      .withTag("tiger:cfcc", "A63")
      .withTag("tiger:county", "DeKalb, GA")
      .withTag("tiger:reviewed", "no")
      .build();

    assertEquals(
      StreetTraversalPermission.ALL,
      WPS.getDataForWay(morelandRamp).forward().getPermission()
    );
  }

  @Test
  public void tenthStreetNE() {
    // For sanity check, secondary roads (e.g. 10th Street) should remain allowed for all modes.
    // https://www.openstreetmap.org/way/505912700
    OsmWay tenthSt = OsmWay.of()
      .withTag("highway", "secondary")
      .withTag("lanes", "4")
      .withTag("maxspeed", "30 mph")
      .withTag("name", "10th Street Northeast")
      .withTag("oneway", "no")
      .withTag("source:maxspeed", "sign")
      .withTag("surface", "asphalt")
      .withTag("tiger:cfcc", "A41")
      .withTag("tiger:county", "Fulton, GA")
      .withTag("tiger:reviewed", "no")
      .build();
    // Some other params omitted.
    assertEquals(StreetTraversalPermission.ALL, WPS.getDataForEntity(tenthSt).getPermission());
  }
}
