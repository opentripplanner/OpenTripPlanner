package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.street.model.StreetTraversalPermission;

class TwinCitiesMapperTest {

  private static final WayPropertySet WPS = new TwinCitiesMapper().buildWayPropertySet();

  @ParameterizedTest
  @ValueSource(strings = { "Saint Paul Skyway", "Minneapolis Skyway" })
  void removeSkyway(String name) {
    var way = OsmWay.of().withTag("highway", "footway").withTag("name", name).build();

    assertEquals(StreetTraversalPermission.NONE, WPS.getDataForEntity(way).getPermission());
  }

  @Test
  void regularFootway() {
    var way = OsmWay.of().withTag("highway", "footway").build();

    assertEquals(StreetTraversalPermission.PEDESTRIAN, WPS.getDataForEntity(way).getPermission());
  }
}
