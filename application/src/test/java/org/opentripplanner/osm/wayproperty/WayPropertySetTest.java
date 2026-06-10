package org.opentripplanner.osm.wayproperty;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.model.TraverseDirection.BACKWARD;
import static org.opentripplanner.osm.model.TraverseDirection.FORWARD;
import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;

import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmTestEntity;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.osm.wayproperty.specifier.BestMatchSpecifier;
import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;

class WayPropertySetTest {

  private static final float EPSILON = 0.01f;

  @Test
  public void mixin() {
    var builder = WayPropertySet.of();
    builder.setProperties("tag=imaginary", withModes(CAR).bicycleSafety(2));
    builder.setMixinProperties("foo=bar", ofBicycleSafety(0.5));
    var wps = builder.build();

    var withoutFoo = new OsmTestEntity("tag", "imaginary");
    assertEquals(2, wps.getDataForEntity(withoutFoo).bicycleSafety());

    // the mixin for foo=bar reduces the bike safety factor
    var withFoo = new OsmTestEntity(entry("tag", "imaginary"), entry("foo", "bar"));
    assertEquals(1, wps.getDataForEntity(withFoo).bicycleSafety());
  }

  @Test
  void speedPicker() {
    // test with no maxspeed tags
    var builder = WayPropertySet.of();
    builder.addSpeedPicker(getSpeedPicker("highway=motorway", kmhAsMs(100)));
    builder.addSpeedPicker(getSpeedPicker("highway=*", kmhAsMs(35)));
    builder.addSpeedPicker(getSpeedPicker("surface=gravel", kmhAsMs(10)));
    builder.setDefaultCarSpeed(kmhAsMs(25));
    var wps = builder.build();

    var way = new OsmTestEntity();

    // test default speeds
    assertEquals(kmhAsMs(25), wps.getCarSpeedForWay(way, FORWARD), EPSILON);
    assertEquals(kmhAsMs(25), wps.getCarSpeedForWay(way, BACKWARD), EPSILON);

    var tertiary = new OsmTestEntity("highway", "tertiary");
    assertEquals(kmhAsMs(35), wps.getCarSpeedForWay(tertiary, FORWARD), EPSILON);
    assertEquals(kmhAsMs(35), wps.getCarSpeedForWay(tertiary, BACKWARD), EPSILON);

    var gravel = new OsmTestEntity(entry("highway", "tertiary"), entry("surface", "gravel"));
    assertEquals(kmhAsMs(10), wps.getCarSpeedForWay(gravel, FORWARD), EPSILON);
    assertEquals(kmhAsMs(10), wps.getCarSpeedForWay(gravel, BACKWARD), EPSILON);

    var motorway = new OsmTestEntity("highway", "motorway");
    assertEquals(kmhAsMs(100), wps.getCarSpeedForWay(motorway, FORWARD), EPSILON);
    assertEquals(kmhAsMs(100), wps.getCarSpeedForWay(motorway, BACKWARD), EPSILON);

    // make sure that 0-speed ways can't exist
    var zeroSpeed = new OsmTestEntity("maxspeed", "0");
    assertEquals(kmhAsMs(25), wps.getCarSpeedForWay(zeroSpeed, FORWARD), EPSILON);
    assertEquals(kmhAsMs(25), wps.getCarSpeedForWay(zeroSpeed, BACKWARD), EPSILON);
  }

  @Test
  public void testWayDataSet() {
    OsmWay way = OsmWay.of()
      .withTag("highway", "footway")
      .withTag("cycleway", "lane")
      .withTag("surface", "gravel")
      .build();

    WayPropertySetBuilder builder = WayPropertySet.of();

    // where there are no way specifiers, the default is used
    var wayPropertySet = builder.build();
    var wayData = wayPropertySet.getDataForWay(way);
    assertEquals(ALL, wayData.forward().getPermission());
    assertEquals(1.0, wayData.forward().walkSafety());
    assertEquals(1.0, wayData.backward().walkSafety());
    assertEquals(1.0, wayData.forward().bicycleSafety());
    assertEquals(1.0, wayData.backward().bicycleSafety());

    // add two equal matches: lane only...
    OsmSpecifier lane_only = new BestMatchSpecifier("cycleway=lane");

    WayProperties lane_is_safer = withModes(ALL).bicycleSafety(1.5).walkSafety(1.0).build();

    builder.addProperties(lane_only, lane_is_safer);

    // and footway only
    OsmSpecifier footway_only = new BestMatchSpecifier("highway=footway");

    WayProperties footways_allow_peds = new WayPropertiesBuilder(PEDESTRIAN).build();

    builder.addProperties(footway_only, footways_allow_peds);

    wayPropertySet = builder.build();
    var dataForWay = wayPropertySet.getDataForWay(way);
    // the first one is found
    assertEquals(lane_is_safer, dataForWay.forward());
    assertEquals(lane_is_safer, dataForWay.backward());

    // add a better match
    OsmSpecifier lane_and_footway = new BestMatchSpecifier("cycleway=lane;highway=footway");

    WayProperties safer_and_peds = new WayPropertiesBuilder(PEDESTRIAN)
      .bicycleSafety(0.75)
      .walkSafety(1.0)
      .build();

    builder.addProperties(lane_and_footway, safer_and_peds);
    wayPropertySet = builder.build();
    dataForWay = wayPropertySet.getDataForWay(way);
    assertEquals(new WayPropertiesPair(safer_and_peds, safer_and_peds), dataForWay);

    // add a mixin
    BestMatchSpecifier gravel = new BestMatchSpecifier("surface=gravel");
    var gravel_is_dangerous = MixinPropertiesBuilder.ofBicycleSafety(2);
    builder.setMixinProperties(gravel, gravel_is_dangerous);

    wayPropertySet = builder.build();
    dataForWay = wayPropertySet.getDataForWay(way);
    assertEquals(1.5, dataForWay.forward().bicycleSafety());

    // test a left-right distinction
    way = OsmWay.of()
      .withTag("highway", "footway")
      .withTag("cycleway", "lane")
      .withTag("cycleway:right", "track")
      .build();

    OsmSpecifier track_only = new BestMatchSpecifier("highway=footway;cycleway=track");
    WayProperties track_is_safest = new WayPropertiesBuilder(ALL)
      .bicycleSafety(0.25)
      .walkSafety(1.0)
      .build();

    builder.addProperties(track_only, track_is_safest);
    wayPropertySet = builder.build();
    dataForWay = wayPropertySet.getDataForWay(way);
    // right (with traffic) comes from track
    assertEquals(0.25, dataForWay.forward().bicycleSafety());
    // left comes from lane
    assertEquals(0.75, dataForWay.backward().bicycleSafety());

    way = OsmWay.of()
      .withTag("highway", "footway")
      .withTag("footway", "sidewalk")
      .withTag("RLIS:reviewed", "no")
      .build();
    WayPropertySetBuilder builder2 = WayPropertySet.of();
    CreativeNamer namer = new CreativeNamer("platform");
    builder2.addCreativeNamer(
      new BestMatchSpecifier("railway=platform;highway=footway;footway=sidewalk"),
      namer
    );
    namer = new CreativeNamer("sidewalk");
    builder2.addCreativeNamer(new BestMatchSpecifier("highway=footway;footway=sidewalk"), namer);
    WayPropertySet propset = builder2.build();
    assertEquals("sidewalk", propset.getCreativeName(way).toString());
  }

  /**
   * Convert kilometers per hour to meters per second
   */
  private float kmhAsMs(float kmh) {
    return (kmh * 1000) / 3600;
  }

  /**
   * Convenience function to get a speed picker for a given specifier and speed.
   *
   * @param specifier The OSM specifier, e.g. highway=motorway
   * @param speed     The speed, in meters per second
   */
  private SpeedPicker getSpeedPicker(String specifier, float speed) {
    return new SpeedPicker(new BestMatchSpecifier(specifier), speed);
  }
}
