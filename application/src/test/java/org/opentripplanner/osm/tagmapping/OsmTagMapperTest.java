package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWithTags;
import org.opentripplanner.osm.wayproperty.WayPropertySet;

public class OsmTagMapperTest {

  @Test
  public void isMotorThroughTrafficExplicitlyDisallowed() {
    OsmWithTags o = new OsmWithTags();
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

    var slowWay = new OsmWithTags();
    slowWay.addTag("highway", "residential");
    assertEquals(20f, osmTagMapper.getCarSpeedForWay(slowWay, true));

    var fastWay = new OsmWithTags();
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

    var withoutFoo = new OsmWithTags();
    withoutFoo.addTag("tag", "imaginary");
    assertEquals(2, wps.getDataForWay(withoutFoo).bicycleSafety().back());

    // the mixin for foo=bar reduces the bike safety factor
    var withFoo = new OsmWithTags();
    withFoo.addTag("tag", "imaginary");
    withFoo.addTag("foo", "bar");
    assertEquals(1, wps.getDataForWay(withFoo).bicycleSafety().back());
  }

  @Test
  public void testAccessNo() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "no");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testAccessPrivate() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testFootModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");
    tags.addTag("foot", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDenied() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("vehicle", "destination");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedMotorVehiclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("motor_vehicle", "designated");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedBicyclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("bicycle", "designated");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testMotorcycleModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");
    tags.addTag("motor_vehicle", "yes");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicycleModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicyclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new DefaultMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "permissive");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  public OsmWithTags way(String key, String value) {
    var way = new OsmWithTags();
    way.addTag(key, value);
    return way;
  }
}
