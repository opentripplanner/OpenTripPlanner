package org.opentripplanner.osm.wayproperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.osm.wayproperty.MixinPropertiesBuilder.ofBicycleSafety;
import static org.opentripplanner.osm.wayproperty.WayPropertiesBuilder.withModes;
import static org.opentripplanner.street.model.StreetTraversalPermission.ALL;
import static org.opentripplanner.street.model.StreetTraversalPermission.CAR;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN;
import static org.opentripplanner.street.model.StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.wayproperty.specifier.BestMatchSpecifier;
import org.opentripplanner.osm.wayproperty.specifier.WayTestData;

public class MapperTest {

  private WayPropertySet wps;
  private OsmTagMapper mapper;
  float epsilon = 0.01f;

  @BeforeEach
  public void setup() {
    var wps = new WayPropertySet();
    var source = new OsmTagMapper();
    source.populateProperties(wps);
    this.wps = wps;
    this.mapper = source;
  }

  /**
   * Test that car speeds are calculated accurately
   */
  @Test
  public void testCarSpeeds() {
    OsmEntity way;

    way = new OsmEntity();
    way.addTag("maxspeed", "60");
    assertTrue(within(kmhAsMs(60), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(60), wps.getCarSpeedForWay(way, true), epsilon));

    way = new OsmEntity();
    way.addTag("maxspeed:forward", "80");
    way.addTag("maxspeed:backward", "20");
    way.addTag("maxspeed", "40");
    assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(20), wps.getCarSpeedForWay(way, true), epsilon));

    way = new OsmEntity();
    way.addTag("maxspeed", "40");
    way.addTag("maxspeed:lanes", "60|80|40");
    assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, true), epsilon));

    way = new OsmEntity();
    way.addTag("maxspeed", "20");
    way.addTag("maxspeed:motorcar", "80");
    assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(80), wps.getCarSpeedForWay(way, true), epsilon));

    // test with english units
    way = new OsmEntity();
    way.addTag("maxspeed", "35 mph");
    assertTrue(within(kmhAsMs(35 * 1.609f), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(35 * 1.609f), wps.getCarSpeedForWay(way, true), epsilon));

    // test with no maxspeed tags
    wps = new WayPropertySet();
    wps.addSpeedPicker(getSpeedPicker("highway=motorway", kmhAsMs(100)));
    wps.addSpeedPicker(getSpeedPicker("highway=*", kmhAsMs(35)));
    wps.addSpeedPicker(getSpeedPicker("surface=gravel", kmhAsMs(10)));
    wps.defaultCarSpeed = kmhAsMs(25);

    way = new OsmEntity();

    // test default speeds
    assertTrue(within(kmhAsMs(25), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(25), wps.getCarSpeedForWay(way, true), epsilon));

    way.addTag("highway", "tertiary");
    assertTrue(within(kmhAsMs(35), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(35), wps.getCarSpeedForWay(way, true), epsilon));

    way = new OsmEntity();
    way.addTag("surface", "gravel");
    assertTrue(within(kmhAsMs(10), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(10), wps.getCarSpeedForWay(way, true), epsilon));

    way = new OsmEntity();
    way.addTag("highway", "motorway");
    assertTrue(within(kmhAsMs(100), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(100), wps.getCarSpeedForWay(way, true), epsilon));

    // make sure that 0-speed ways can't exist
    way = new OsmEntity();
    way.addTag("maxspeed", "0");
    assertTrue(within(kmhAsMs(25), wps.getCarSpeedForWay(way, false), epsilon));
    assertTrue(within(kmhAsMs(25), wps.getCarSpeedForWay(way, true), epsilon));

    assertSpeed(1.3889, "5");
    assertSpeed(1.3889, "5 kmh");
    assertSpeed(1.3889, " 5 kmh ");
    assertSpeed(1.3889, " 5 ");
    assertSpeed(4.166669845581055, "15");
    assertSpeed(4.305559158325195, "15.5");
    assertSpeed(4.305559158325195, "15.5 kmh");
    assertSpeed(4.305559158325195, "15.5 kph");
    assertSpeed(4.305559158325195, "15.5 km/h");
    assertSpeed(22.347200393676758, "50 mph");
    assertSpeed(22.347200393676758, "50.0 mph");

    assertEquals(wps.maxUsedCarSpeed, mapper.getMaxUsedCarSpeed(wps));
  }

  @Test
  void stairs() {
    // there is no special handling for stairs with ramps yet
    var props = wps.getDataForWay(WayTestData.stairs());
    assertEquals(PEDESTRIAN, props.getPermission());
  }

  @Test
  void footDiscouraged() {
    var regular = WayTestData.pedestrianTunnel();
    var props = wps.getDataForWay(regular);
    assertEquals(PEDESTRIAN_AND_BICYCLE, props.getPermission());
    assertEquals(1, props.walkSafety().forward());

    var discouraged = WayTestData.pedestrianTunnel().addTag("foot", "discouraged");
    var discouragedProps = wps.getDataForWay(discouraged);
    assertEquals(PEDESTRIAN_AND_BICYCLE, discouragedProps.getPermission());
    assertEquals(3, discouragedProps.walkSafety().forward());
  }

  @Test
  void bicycleDiscouraged() {
    var regular = WayTestData.southeastLaBonitaWay();
    var props = wps.getDataForWay(regular);
    assertEquals(ALL, props.getPermission());
    assertEquals(.98, props.bicycleSafety().forward());

    var discouraged = WayTestData.southeastLaBonitaWay().addTag("bicycle", "discouraged");
    var discouragedProps = wps.getDataForWay(discouraged);
    assertEquals(ALL, discouragedProps.getPermission());
    assertEquals(2.94, discouragedProps.bicycleSafety().forward(), epsilon);
  }

  @Test
  void footUseSidepath() {
    var regular = WayTestData.pedestrianTunnel();
    var props = wps.getDataForWay(regular);
    assertEquals(PEDESTRIAN_AND_BICYCLE, props.getPermission());
    assertEquals(1, props.walkSafety().forward());

    var useSidepath = WayTestData.pedestrianTunnel().addTag("foot", "use_sidepath");
    var useSidepathProps = wps.getDataForWay(useSidepath);
    assertEquals(PEDESTRIAN_AND_BICYCLE, useSidepathProps.getPermission());
    assertEquals(5, useSidepathProps.walkSafety().forward());
  }

  @Test
  void bicycleUseSidepath() {
    var regular = WayTestData.southeastLaBonitaWay();
    var props = wps.getDataForWay(regular);
    assertEquals(ALL, props.getPermission());
    assertEquals(.98, props.bicycleSafety().forward());

    var useSidepath = WayTestData.southeastLaBonitaWay().addTag("bicycle", "use_sidepath");
    var useSidepathProps = wps.getDataForWay(useSidepath);
    assertEquals(ALL, useSidepathProps.getPermission());
    assertEquals(4.9, useSidepathProps.bicycleSafety().forward(), epsilon);

    var useSidepathForward = WayTestData
      .southeastLaBonitaWay()
      .addTag("bicycle:forward", "use_sidepath");
    var useSidepathForwardProps = wps.getDataForWay(useSidepathForward);
    assertEquals(ALL, useSidepathForwardProps.getPermission());
    assertEquals(4.9, useSidepathForwardProps.bicycleSafety().forward(), epsilon);
    assertEquals(0.98, useSidepathForwardProps.bicycleSafety().back(), epsilon);

    var useSidepathBackward = WayTestData
      .southeastLaBonitaWay()
      .addTag("bicycle:backward", "use_sidepath");
    var useSidepathBackwardProps = wps.getDataForWay(useSidepathBackward);
    assertEquals(ALL, useSidepathBackwardProps.getPermission());
    assertEquals(0.98, useSidepathBackwardProps.bicycleSafety().forward(), epsilon);
    assertEquals(4.9, useSidepathBackwardProps.bicycleSafety().back(), epsilon);
  }

  @Test
  void slopeOverrides() {
    var regular = WayTestData.southeastLaBonitaWay();
    assertFalse(wps.getSlopeOverride(regular));

    var indoor = WayTestData.southeastLaBonitaWay().addTag("indoor", "yes");
    assertTrue(wps.getSlopeOverride(indoor));
  }

  @Test
  public void mixin() {
    wps.setProperties("tag=imaginary", withModes(CAR).bicycleSafety(2));
    wps.setMixinProperties("foo=bar", ofBicycleSafety(0.5));

    var withoutFoo = new OsmEntity();
    withoutFoo.addTag("tag", "imaginary");
    assertEquals(2, wps.getDataForWay(withoutFoo).bicycleSafety().back());

    // the mixin for foo=bar reduces the bike safety factor
    var withFoo = new OsmEntity();
    withFoo.addTag("tag", "imaginary");
    withFoo.addTag("foo", "bar");
    assertEquals(1, wps.getDataForWay(withFoo).bicycleSafety().back());
  }

  /**
   * Test that two values are within epsilon of each other.
   */
  private boolean within(float val1, float val2, float epsilon) {
    return (Math.abs(val1 - val2) < epsilon);
  }

  /**
   * Convert kilometers per hour to meters per second
   */
  private float kmhAsMs(float kmh) {
    return kmh * 1000 / 3600;
  }

  /**
   * Convenience function to get a speed picker for a given specifier and speed.
   *
   * @param specifier The OSM specifier, e.g. highway=motorway
   * @param speed     The speed, in meters per second
   */
  private SpeedPicker getSpeedPicker(String specifier, float speed) {
    SpeedPicker sp = new SpeedPicker();
    sp.specifier = new BestMatchSpecifier(specifier);
    sp.speed = speed;
    return sp;
  }

  private void assertSpeed(double v, String s) {
    assertEquals(v, wps.getMetersSecondFromSpeed(s), epsilon);
  }
}
