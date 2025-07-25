package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    var corridor = wps.getDataForEntity(WayTestData.indoor("corridor"), null);
    assertEquals(PEDESTRIAN, corridor.getPermission());
    var area = wps.getDataForEntity(WayTestData.indoor("area"), null);
    assertEquals(PEDESTRIAN, area.getPermission());
  }

  @Test
  void cycleway() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.cycleway(), null).getPermission()
    );
  }

  @Test
  void bridleway() {
    assertEquals(
      PEDESTRIAN_AND_BICYCLE,
      wps.getDataForEntity(WayTestData.bridleway(), null).getPermission()
    );
  }
}
