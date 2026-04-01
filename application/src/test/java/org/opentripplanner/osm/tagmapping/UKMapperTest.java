package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

public class UKMapperTest {

  static final WayPropertySet WPS;

  static {
    WPS = new UKMapper().buildWayPropertySet();
  }

  @Test
  void cycleway() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(WayTestData.cycleway()).getPermission()
    );
  }

  @Test
  void bridleway() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      WPS.getDataForEntity(WayTestData.bridleway()).getPermission()
    );
  }

  @Test
  void trunk() {
    var way = WayTestData.highwayTrunk();
    assertEquals(ALL, WPS.getDataForWay(way).forward().getPermission());
    assertEquals(2.5, WPS.getDataForWay(way).forward().walkSafety());
    assertEquals(2.5, WPS.getDataForWay(way).forward().bicycleSafety());
    way.addTag("oneway", "yes");
    way.addTag("expressway", "yes");
    assertEquals(12.5, WPS.getDataForWay(way).forward().walkSafety());
    assertEquals(12.5, WPS.getDataForWay(way).forward().bicycleSafety());
  }
}
