package org.opentripplanner.osm.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.cyclewayLaneTrack;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.cyclewayLeft;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.model.OsmEntity;

class BestMatchSpecifierTest extends SpecifierTest {

  OsmSpecifier highwayPrimary = new BestMatchSpecifier("highway=primary");
  OsmSpecifier pedestrianUndergroundTunnel = new BestMatchSpecifier(
    "highway=footway;layer=-1;tunnel=yes;indoor=yes"
  );

  static OsmSpecifier bikeLane = new BestMatchSpecifier("highway=residential;cycleway=lane");
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
    assertEquals(210, result.backward());
    assertEquals(100, result.forward());
  }

  static Stream<Arguments> leftRightTestCases() {
    return Stream.of(
      Arguments.of(cyclewayLeft(), bikeLane, 210, 100),
      Arguments.of(cyclewayLaneTrack(), cyclewayTrack, 100, 210),
      Arguments.of(cyclewayLaneTrack(), highwayFootwayCyclewayLane, 210, 100),
      Arguments.of(cyclewayLaneTrack(), cyclewayLane, 110, 0)
    );
  }

  @ParameterizedTest(
    name = "way {0} with specifier {1} should have a backward score {2} and forward score {3}"
  )
  @MethodSource("leftRightTestCases")
  void leftRight(OsmEntity way, OsmSpecifier spec, int expectedBackward, int expectedForward) {
    var result = spec.matchScores(way);
    assertEquals(expectedBackward, result.backward());
    assertEquals(expectedForward, result.forward());
  }
}
