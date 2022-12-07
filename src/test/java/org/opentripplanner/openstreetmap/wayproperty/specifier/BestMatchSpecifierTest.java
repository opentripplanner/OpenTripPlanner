package org.opentripplanner.openstreetmap.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.cobblestones;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.cyclewayLaneTrack;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.cyclewayLeft;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.flattenedCobblestones;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.test.support.VariableSource;

class BestMatchSpecifierTest extends SpecifierTest {

  OsmSpecifier highwayPrimary = new BestMatchSpecifier("highway=primary");
  OsmSpecifier pedestrianUndergroundTunnel = new BestMatchSpecifier(
    "highway=footway;layer=-1;tunnel=yes;indoor=yes"
  );

  static OsmSpecifier bikeLane = new BestMatchSpecifier("highway=residential;cycleway=lane");
  static OsmSpecifier regularCobblestones = new BestMatchSpecifier(
    "highway=residential;surface=cobblestones"
  );
  static OsmSpecifier cobblestonesFlattened = new BestMatchSpecifier(
    "highway=residential;surface=cobblestones:flattened"
  );

  static OsmSpecifier cyclewayTrack = new BestMatchSpecifier("highway=footway;cycleway=track");
  static OsmSpecifier highwayFootwayCyclewayLane = new BestMatchSpecifier(
    "highway=footway;cycleway=lane"
  );
  static OsmSpecifier cyclewayLane = new BestMatchSpecifier("cycleway=lane");

  @Test
  void carTunnel() {
    var tunnel = WayTestData.carTunnel();
    assertScore(110, highwayPrimary, tunnel);
    assertScore(200, pedestrianUndergroundTunnel, tunnel);
  }

  @Test
  void pedestrianTunnel() {
    var tunnel = WayTestData.pedestrianTunnel();

    assertScore(0, highwayPrimary, tunnel);
    assertScore(410, pedestrianUndergroundTunnel, tunnel);
  }

  @Test
  void leftRightMatch() {
    var way = WayTestData.cyclewayLeft();
    var result = bikeLane.matchScores(way);
    assertEquals(210, result.left());
    assertEquals(100, result.right());
  }

  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(flattenedCobblestones(), regularCobblestones, 100),
    Arguments.of(flattenedCobblestones(), cobblestonesFlattened, 210),
    Arguments.of(cobblestones(), regularCobblestones, 210),
    Arguments.of(cobblestones(), cobblestonesFlattened, 185)
  );

  @ParameterizedTest(name = "way {0} with specifier {1} should have a score of {2}")
  @VariableSource("testCases")
  void cobblestonesFlattened(OSMWithTags way, OsmSpecifier spec, int expectedScore) {
    var result = spec.matchScores(way);
    assertEquals(expectedScore, result.left());
    assertEquals(expectedScore, result.right());
  }

  static Stream<Arguments> leftRightTestCases = Stream.of(
    Arguments.of(cyclewayLeft(), bikeLane, 210, 100),
    Arguments.of(cyclewayLaneTrack(), cyclewayTrack, 100, 210),
    Arguments.of(cyclewayLaneTrack(), highwayFootwayCyclewayLane, 210, 100),
    Arguments.of(cyclewayLaneTrack(), cyclewayLane, 110, 0)
  );

  @ParameterizedTest(
    name = "way {0} with specifier {1} should have a left score {2} and right score {3}"
  )
  @VariableSource("leftRightTestCases")
  void leftRight(OSMWithTags way, OsmSpecifier spec, int expectedLeft, int expectedRight) {
    var result = spec.matchScores(way);
    assertEquals(expectedLeft, result.left());
    assertEquals(expectedRight, result.right());
  }
}
