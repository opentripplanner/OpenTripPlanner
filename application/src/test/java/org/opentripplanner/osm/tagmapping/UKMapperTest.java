package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

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
}
