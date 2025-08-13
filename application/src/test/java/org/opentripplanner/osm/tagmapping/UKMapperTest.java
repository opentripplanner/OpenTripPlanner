package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

public class UKMapperTest {

  static WayPropertySet wps = new WayPropertySet();

  static {
    var source = new UKMapper();
    source.populateProperties(wps);
  }

  @Test
  void indoor() {
    var corridor = wps.getDataForWay(WayTestData.indoor("corridor"));
    assertEquals(PEDESTRIAN, corridor.getPermission());
    var area = wps.getDataForWay(WayTestData.indoor("area"));
    assertEquals(PEDESTRIAN, area.getPermission());
  }

  @Test
  void cycleway() {
    assertEquals(PEDESTRIAN_AND_BICYCLE, wps.getDataForWay(WayTestData.cycleway()).getPermission());
  }

  @Test
  void bridleway() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForWay(WayTestData.bridleway()).getPermission()
    );
  }

  @Test
  void trunk() {
    var way = WayTestData.highwayTrunk();
    assertEquals(ALL, wps.getDataForWay(way).getPermission());
    assertEquals(2.5, wps.getDataForWay(way).walkSafety().forward());
    assertEquals(2.5, wps.getDataForWay(way).bicycleSafety().forward());
    way.addTag("oneway", "yes");
    way.addTag("expressway", "yes");
    assertEquals(12.5, wps.getDataForWay(way).walkSafety().forward());
    assertEquals(12.5, wps.getDataForWay(way).bicycleSafety().forward());
  }
}
