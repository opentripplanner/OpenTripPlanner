package org.opentripplanner.osm.tagmapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmWithTags;

public class OsmTagMapperTest {

  @Test
  public void isMotorThroughTrafficExplicitlyDisallowed() {
    OsmWithTags o = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

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
    OsmTagMapper osmTagMapper = new OsmTagMapper();
    assertTrue(
      osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(way("bicycle", "destination"))
    );
    assertTrue(
      osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(way("access", "destination"))
    );
  }

  @Test
  public void isWalkNoThroughTrafficExplicitlyDisallowed() {
    OsmTagMapper osmTagMapper = new OsmTagMapper();
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(way("foot", "destination")));
    assertTrue(
      osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(way("access", "destination"))
    );
  }

  @Test
  public void testAccessNo() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "no");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testAccessPrivate() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testFootModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("foot", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDenied() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedMotorVehiclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("motor_vehicle", "designated");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedBicyclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("bicycle", "designated");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testMotorcycleModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("motor_vehicle", "yes");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicycleModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleNoThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkNoThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicyclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

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
