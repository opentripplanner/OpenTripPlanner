package org.opentripplanner.graph_builder.module.osm.specifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult.EXACT;
import static org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult.NONE;
import static org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult.PARTIAL;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.Equals;
import org.opentripplanner.graph_builder.module.osm.specifier.Operation.MatchResult;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.test.support.VariableSource;

class OperationTest {

  static Operation cyclewayLane = new Equals("cycleway", "lane");
  static Stream<Arguments> testCases = Stream.of(
    Arguments.of(WayTestData.cyclewayLeft(), cyclewayLane, EXACT, NONE),
    Arguments.of(WayTestData.cyclewayLaneTrack(), cyclewayLane, PARTIAL, NONE)
  );

  @ParameterizedTest(name = "way {0} with op {1} should have a left result {2}, right result {3}")
  @VariableSource("testCases")
  void matchesLeftRight(
    OSMWithTags way,
    Operation op,
    MatchResult leftExpectation,
    MatchResult rightExpectation
  ) {
    assertEquals(leftExpectation, op.matchLeft(way));
    assertEquals(rightExpectation, op.matchRight(way));
  }
}
