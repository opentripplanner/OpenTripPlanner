package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.of;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.CAR;
import static org.opentripplanner.routing.edgetype.StreetTraversalPermission.NONE;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

public class WayPropertySetSourceTest {

  @Test
  public void isMotorThroughTrafficExplicitlyDisallowed() {
    OSMWithTags o = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    assertFalse(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "something");
    assertFalse(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "destination");
    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "private");
    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    assertTrue(
      wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(
        way("motor_vehicle", "destination")
      )
    );
  }

  @Test
  public void isBicycleNoThroughTrafficExplicitlyDisallowed() {
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();
    assertTrue(
      wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(
        way("bicycle", "destination")
      )
    );
    assertTrue(
      wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(
        way("access", "destination")
      )
    );
  }

  @Test
  public void isWalkNoThroughTrafficExplicitlyDisallowed() {
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();
    assertTrue(
      wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(way("foot", "destination"))
    );
    assertTrue(
      wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(way("access", "destination"))
    );
  }

  @Test
  public void mixin() {
    var source = new DefaultWayPropertySetSource();
    var wps = new WayPropertySet();

    wps.setProperties("tag=imaginary", of(CAR).bicycleSafety(2));

    wps.setMixinProperties("foo=bar", of(NONE).bicycleSafety(0.5));
    source.populateProperties(wps);

    var withoutFoo = new OSMWithTags();
    withoutFoo.addTag("tag", "imaginary");
    assertEquals(2, wps.getDataForWay(withoutFoo).getBicycleSafetyFeatures().second);

    // the mixin for foo=bar reduces the bike safety factor
    var withFoo = new OSMWithTags();
    withFoo.addTag("tag", "imaginary");
    withFoo.addTag("foo", "bar");
    assertEquals(1, wps.getDataForWay(withFoo).getBicycleSafetyFeatures().second);
  }

  public OSMWithTags way(String key, String value) {
    var way = new OSMWithTags();
    way.addTag(key, value);
    return way;
  }
}
