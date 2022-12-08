package org.opentripplanner.openstreetmap.wayproperty.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.EXACT;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult.NONE;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.carTunnel;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.cobblestones;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.cycleway;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.cyclewayLaneTrack;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.cyclewayLeft;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.fiveLanes;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.pedestrianTunnel;
import static org.opentripplanner.openstreetmap.wayproperty.specifier.WayTestData.threeLanes;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.Absent;
import org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.Equals;
import org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.GreaterThan;
import org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.LessThan;
import org.opentripplanner.openstreetmap.wayproperty.specifier.Condition.MatchResult;
import org.opentripplanner.test.support.VariableSource;

class ConditionTest {

  static Condition cyclewayLane = new Equals("cycleway", "lane");
  static Condition cyclewayTrack = new Equals("cycleway", "track");

  static Condition cyclewayAbsent = new Absent("cycleway");
  static Condition moreThanFourLanes = new GreaterThan("lanes", 4);
  static Condition lessThanFourLanes = new LessThan("lanes", 4);

  static Stream<Arguments> equalsCases = Stream.of(
    Arguments.of(cyclewayLeft(), cyclewayLane, EXACT, NONE),
    Arguments.of(cyclewayLaneTrack(), cyclewayLane, EXACT, NONE),
    Arguments.of(cyclewayLaneTrack(), cyclewayTrack, NONE, EXACT)
  );

  @ParameterizedTest(name = "way {0} with op {1} should have a left result {2}, right result {3}")
  @VariableSource("equalsCases")
  void leftRight(
    OSMWithTags way,
    Condition op,
    MatchResult leftExpectation,
    MatchResult rightExpectation
  ) {
    assertEquals(leftExpectation, op.matchLeft(way));
    assertEquals(rightExpectation, op.matchRight(way));
  }

  static Stream<Arguments> otherCases = Stream.of(
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
    Arguments.of(cycleway(), lessThanFourLanes, NONE)
  );

  @ParameterizedTest(name = "way {0} with op {1} should have a result {2}")
  @VariableSource("otherCases")
  void otherTests(OSMWithTags way, Condition op, MatchResult expectation) {
    assertEquals(expectation, op.match(way));
  }
}
