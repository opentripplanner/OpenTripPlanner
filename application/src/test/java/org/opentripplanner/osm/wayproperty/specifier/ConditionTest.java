package org.opentripplanner.osm.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.EXACT;
import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.NONE;
import static org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult.WILDCARD;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.carTunnel;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.cobblestones;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.cycleway;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.cyclewayBoth;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.cyclewayLaneTrack;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.cyclewayLeft;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.excellentSmoothness;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.fiveLanes;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.highwayTertiary;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.noSidewalk;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.pedestrianTunnel;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.sidewalkBoth;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.threeLanes;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.tramsForward;
import static org.opentripplanner.osm.wayproperty.specifier.WayTestData.veryBadSmoothness;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.osm.model.OsmEntity;
import org.opentripplanner.osm.wayproperty.specifier.Condition.Absent;
import org.opentripplanner.osm.wayproperty.specifier.Condition.Equals;
import org.opentripplanner.osm.wayproperty.specifier.Condition.GreaterThan;
import org.opentripplanner.osm.wayproperty.specifier.Condition.InclusiveRange;
import org.opentripplanner.osm.wayproperty.specifier.Condition.LessThan;
import org.opentripplanner.osm.wayproperty.specifier.Condition.MatchResult;
import org.opentripplanner.osm.wayproperty.specifier.Condition.OneOf;
import org.opentripplanner.osm.wayproperty.specifier.Condition.Present;

class ConditionTest {

  static Condition cyclewayLane = new Equals("cycleway", "lane");
  static Condition cyclewayTrack = new Equals("cycleway", "track");
  static Condition embeddedTrams = new Equals("embedded_rails", "tram");
  static Condition cyclewayPresent = new Present("cycleway");
  static Condition cyclewayAbsent = new Absent("cycleway");
  static Condition moreThanFourLanes = new GreaterThan("lanes", 4);
  static Condition lessThanFourLanes = new LessThan("lanes", 4);
  static Condition betweenFiveAndThreeLanes = new InclusiveRange("lanes", 5, 3);
  static Condition smoothnessBadAndWorseThanBad = new OneOf(
    "smoothness",
    "bad",
    "very_bad",
    "horrible",
    "very_horrible",
    "impassable"
  );
  static Condition noSidewalk = new Condition.OneOfOrAbsent("sidewalk");

  static Stream<Arguments> equalsCases() {
    return Stream.of(
      Arguments.of(cyclewayLeft(), cyclewayLane, EXACT, NONE),
      Arguments.of(cyclewayLaneTrack(), cyclewayLane, EXACT, NONE),
      Arguments.of(cyclewayBoth(), cyclewayLane, EXACT, EXACT),
      Arguments.of(cyclewayLaneTrack(), cyclewayTrack, NONE, EXACT),
      Arguments.of(tramsForward(), embeddedTrams, NONE, EXACT)
    );
  }

  @ParameterizedTest(
    name = "way {0} with op {1} should have a backward result {2}, forward result {3}"
  )
  @MethodSource("equalsCases")
  void leftRight(
    OsmEntity way,
    Condition op,
    MatchResult backwardExpectation,
    MatchResult forwardExpectation
  ) {
    assertEquals(backwardExpectation, op.matchBackward(way));
    assertEquals(forwardExpectation, op.matchForward(way));
  }

  static Stream<Arguments> otherCases() {
    return Stream.of(
      Arguments.of(cycleway(), cyclewayPresent, WILDCARD),
      Arguments.of(carTunnel(), cyclewayPresent, NONE),
      Arguments.of(carTunnel(), cyclewayAbsent, EXACT),
      Arguments.of(cobblestones(), cyclewayAbsent, EXACT),
      Arguments.of(cycleway(), cyclewayAbsent, NONE),
      Arguments.of(cycleway(), moreThanFourLanes, NONE),
      Arguments.of(carTunnel(), moreThanFourLanes, NONE),
      Arguments.of(pedestrianTunnel(), moreThanFourLanes, NONE),
      Arguments.of(fiveLanes(), moreThanFourLanes, EXACT),
      Arguments.of(fiveLanes(), lessThanFourLanes, NONE),
      Arguments.of(threeLanes(), lessThanFourLanes, EXACT),
      Arguments.of(carTunnel(), lessThanFourLanes, NONE),
      Arguments.of(cycleway(), lessThanFourLanes, NONE),
      Arguments.of(fiveLanes(), betweenFiveAndThreeLanes, EXACT),
      Arguments.of(threeLanes(), betweenFiveAndThreeLanes, EXACT),
      Arguments.of(veryBadSmoothness(), smoothnessBadAndWorseThanBad, EXACT),
      Arguments.of(cobblestones(), smoothnessBadAndWorseThanBad, NONE),
      Arguments.of(excellentSmoothness(), smoothnessBadAndWorseThanBad, NONE),
      Arguments.of(noSidewalk(), noSidewalk, EXACT),
      Arguments.of(highwayTertiary(), noSidewalk, EXACT),
      Arguments.of(sidewalkBoth(), noSidewalk, NONE)
    );
  }

  @ParameterizedTest(name = "way {0} with op {1} should have a result {2}")
  @MethodSource("otherCases")
  void otherTests(OsmEntity way, Condition op, MatchResult expectation) {
    assertEquals(expectation, op.match(way));
  }

  @Test
  void assertThrowsOnLowerLowerThanUpperLimit() {
    Assertions.assertThrows(IllegalArgumentException.class, () ->
      new InclusiveRange("lanes", 4, 6)
    );
  }
}
