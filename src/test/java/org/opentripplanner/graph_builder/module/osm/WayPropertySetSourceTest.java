package org.opentripplanner.graph_builder.module.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.graph_builder.module.osm.WayPropertiesBuilder.withModes;
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

    wps.setProperties("tag=imaginary", withModes(CAR).bicycleSafety(2));

    wps.setMixinProperties("foo=bar", withModes(NONE).bicycleSafety(0.5));
    source.populateProperties(wps);

    var withoutFoo = new OSMWithTags();
    withoutFoo.addTag("tag", "imaginary");
    assertEquals(2, wps.getDataForWay(withoutFoo).getBicycleSafetyFeatures().back());

    // the mixin for foo=bar reduces the bike safety factor
    var withFoo = new OSMWithTags();
    withFoo.addTag("tag", "imaginary");
    withFoo.addTag("foo", "bar");
    assertEquals(1, wps.getDataForWay(withFoo).getBicycleSafetyFeatures().back());
  }

  public void testAccessNo() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("access", "no");

    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testAccessPrivate() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("access", "private");

    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testFootModifier() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("access", "private");
    tags.addTag("foot", "yes");

    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDenied() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("vehicle", "destination");

    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedMotorVehiclePermissive() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("vehicle", "destination");
    tags.addTag("motor_vehicle", "designated");

    assertFalse(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedBicyclePermissive() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("vehicle", "destination");
    tags.addTag("bicycle", "designated");

    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testMotorcycleModifier() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("access", "private");
    tags.addTag("motor_vehicle", "yes");

    assertFalse(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicycleModifier() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "yes");

    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicyclePermissive() {
    OSMWithTags tags = new OSMWithTags();
    WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "permissive");

    assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(wayPropertySetSource.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  public OSMWithTags way(String key, String value) {
    var way = new OSMWithTags();
    way.addTag(key, value);
    return way;
  }
}
