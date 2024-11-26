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
  public void isBicycleThroughTrafficExplicitlyDisallowed() {
    OsmTagMapper osmTagMapper = new OsmTagMapper();
    assertTrue(
      osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(way("bicycle", "destination"))
    );
    assertTrue(
      osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(way("access", "destination"))
    );
  }

  @Test
  public void isWalkThroughTrafficExplicitlyDisallowed() {
    OsmTagMapper osmTagMapper = new OsmTagMapper();
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(way("foot", "destination")));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(way("access", "destination")));
  }

  @Test
  public void testAccessNo() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "no");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testAccessPrivate() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testFootModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("foot", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDenied() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedMotorVehiclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("motor_vehicle", "designated");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testVehicleDeniedBicyclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("vehicle", "destination");
    tags.addTag("bicycle", "designated");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testMotorcycleModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("motor_vehicle", "yes");

    assertFalse(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicycleModifier() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "yes");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  @Test
  public void testBicyclePermissive() {
    OsmWithTags tags = new OsmWithTags();
    OsmTagMapper osmTagMapper = new OsmTagMapper();

    tags.addTag("access", "private");
    tags.addTag("bicycle", "permissive");

    assertTrue(osmTagMapper.isMotorVehicleThroughTrafficExplicitlyDisallowed(tags));
    assertFalse(osmTagMapper.isBicycleThroughTrafficExplicitlyDisallowed(tags));
    assertTrue(osmTagMapper.isWalkThroughTrafficExplicitlyDisallowed(tags));
  }

  public OsmWithTags way(String key, String value) {
    var way = new OsmWithTags();
    way.addTag(key, value);
    return way;
  }
}
