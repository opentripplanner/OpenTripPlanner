package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult.EXACT;
import static org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult.NONE;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.carTunnel;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.cobblestones;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.cycleway;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.cyclewayLaneTrack;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.cyclewayLeft;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.fiveLanes;
import static org.opentripplanner.graph_builder.module.osm.specifier.WayTestData.pedestrianTunnel;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.Absent;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.Equals;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.GreaterThan;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.test.support.VariableSource;

class OperationTest {

  static Operation cyclewayLane = new Equals("cycleway", "lane");
  static Operation cyclewayTrack = new Equals("cycleway", "track");

  static Operation cyclewayAbsent = new Absent("cycleway");
  static Operation moreThanFourLanes = new GreaterThan("lanes", 4);

  static Stream<Arguments> equalsCases = Stream.of(
    Arguments.of(cyclewayLeft(), cyclewayLane, EXACT, NONE),
    Arguments.of(cyclewayLaneTrack(), cyclewayLane, EXACT, NONE),
    Arguments.of(cyclewayLaneTrack(), cyclewayTrack, NONE, EXACT)
  );

  @ParameterizedTest(name = "way {0} with op {1} should have a left result {2}, right result {3}")
  @VariableSource("equalsCases")
  void leftRight(
    OSMWithTags way,
    Operation op,
    MatchResult leftExpectation,
    MatchResult rightExpectation
  ) {
    assertEquals(leftExpectation, op.matchLeft(way));
    assertEquals(rightExpectation, op.matchRight(way));
  }

  static Stream<Arguments> opsCases = Stream.of(
    Arguments.of(carTunnel(), cyclewayAbsent, EXACT),
    Arguments.of(cobblestones(), cyclewayAbsent, EXACT),
    Arguments.of(cycleway(), cyclewayAbsent, NONE),
    Arguments.of(cycleway(), moreThanFourLanes, NONE),
    Arguments.of(carTunnel(), moreThanFourLanes, NONE),
    Arguments.of(pedestrianTunnel(), moreThanFourLanes, NONE),
    Arguments.of(fiveLanes(), moreThanFourLanes, EXACT)
  );

  @ParameterizedTest(name = "way {0} with op {1} should have a result {2}")
  @VariableSource("opsCases")
  void otherOperations(OSMWithTags way, Operation op, MatchResult expectation) {
    assertEquals(expectation, op.match(way));
  }
}
