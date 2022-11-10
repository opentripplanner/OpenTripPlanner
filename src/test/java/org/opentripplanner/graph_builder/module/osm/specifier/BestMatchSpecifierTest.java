package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.cobblestones;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.flattenedCobblestones;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.test.support.VariableSource;

class BestMatchSpecifierTest extends SpecifierTest {

  OsmSpecifier highwayPrimary = new BestMatchSpecifier("highway=primary");
  OsmSpecifier pedestrianUndergroundTunnel = new BestMatchSpecifier(
    "highway=footway;layer=-1;tunnel=yes;indoor=yes"
  );

  OsmSpecifier bikeLane = new BestMatchSpecifier("highway=residential;cycleway=lane");
  static OsmSpecifier regularCobblestones = new BestMatchSpecifier(
    "highway=residential;surface=cobblestones"
  );
  static OsmSpecifier cobblestonesFlattened = new BestMatchSpecifier(
    "highway=residential;surface=cobblestones:flattened"
  );

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
}
