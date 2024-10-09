package org.opentripplanner.openstreetmap.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.openstreetmap.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.openstreetmap.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.WayPropertySet;

public class OsmTagMapperTest {

  @Test
  public void isMotorThroughTrafficExplicitlyDisallowed() {
    OSMWithTags o = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "something");
    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "destination");
    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    o.addTag("access", "private");
    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

    assertTrue(
      osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(
        way("motor_vehicle", "destination")
      )
    );
  }

  @Test
  public void constantSpeedCarRouting() {
    OsmTagMapper osmTagMapper = new ConstantSpeedFinlandMapper(20f);

    var slowWay = new OSMWithTags();
    slowWay.addTag("highway", "residential");
    assertEquals(20f, osmTagMapper.getCarSpeedForWay(slowWay, true));

    var fastWay = new OSMWithTags();
    fastWay.addTag("highway", "motorway");
    fastWay.addTag("maxspeed", "120 kmph");
    assertEquals(20f, osmTagMapper.getCarSpeedForWay(fastWay, true));
  }

  @Test
  public void isBicycleNoThroughTrafficExplicitlyDisallowed() {
    OsmTagMapper osmTagMapper = new DefaultMapper();
    assertTrue(
      osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(way("bicycle", "destination"))
    );
    assertTrue(
      osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(way("access", "destination"))
    );
  }

  @Test
  public void isWalkNoThroughTrafficExplicitlyDisallowed() {
    OsmTagMapper osmTagMapper = new DefaultMapper();
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(way("foot", "destination")));
    assertTrue(
      osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(way("access", "destination"))
    );
  }

  @Test
  public void mixin() {
    var source = new DefaultMapper();
    var wps = new WayPropertySet();

    wps.setProperties("tag=imaginary", withModes(CAR).bicycleSafety(2));

    wps.setMixinProperties("foo=bar", ofBicycleSafety(0.5));
    source.populateProperties(wps);

    var withoutFoo = new OSMWithTags();
    withoutFoo.addTag("tag", "imaginary");
    assertEquals(2, wps.getDataForWay(withoutFoo).bicycleSafety().back());

    // the mixin for foo=bar reduces the bike safety factor
    var withFoo = new OSMWithTags();
    withFoo.addTag("tag", "imaginary");
    withFoo.addTag("foo", "bar");
    assertEquals(1, wps.getDataForWay(withFoo).bicycleSafety().back());
  }

  @Test
  public void testAccessNo() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "no");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testAccessPrivate() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testFootModifier() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");
    tags.addTag("foot", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDenied() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("vehicle", "destination");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedMotorVehiclePermissive() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("motor_vehicle", "designated");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedBicyclePermissive() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("bicycle", "designated");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testMotorcycleModifier() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");
    tags.addTag("motor_vehicle", "yes");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicycleModifier() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicyclePermissive() {
    OSMWithTags tags = new OSMWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "permissive");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  public OSMWithTags way(String key, String value) {
    var way = new OSMWithTags();
    way.addTag(key, value);
    return way;
  }
}
