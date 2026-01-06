package org.opentripplanner.raptor.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor._data.transit.TestAccessEgress;
import org.opentripplanner.utils.time.TimeUtils;

class RaptorAccessEgressToStringParserTest {

  private static final int STOP = 7;
  private static final int COST = 1531;
  private static final int DURATION = 12;
  private static final int T10_30 = TimeUtils.time("10:30");
  private static final int T12_30 = TimeUtils.time("11:30");
  private static final int N_VIAS = 3;
  private static final int N_RIDES = 3;
  private static final int TIME_PENALTY_VALUE = 600;

  private static final TestAccessEgress WALK = TestAccessEgress.walk(STOP, DURATION);
  private static final TestAccessEgress FLEX = TestAccessEgress.flex(STOP, DURATION);
  private static final TestAccessEgress FLEX_AND_WALK = TestAccessEgress.flexAndWalk(
    STOP,
    DURATION
  );
  private static final TestAccessEgress FREE = TestAccessEgress.free(STOP);
  private static final TestAccessEgress OPENING_HOURS = TestAccessEgress.walk(
    STOP,
    DURATION
  ).openingHours(T10_30, T12_30);
  private static final TestAccessEgress VIA = TestAccessEgress.walk(STOP, DURATION);
  private static final TestAccessEgress TIME_PENALTY = TestAccessEgress.walk(
    STOP,
    DURATION
  ).withTimePenalty(TIME_PENALTY_VALUE);
  private static final TestAccessEgress EVERYTHING = TestAccessEgress.flexAndWalk(
    STOP,
    DURATION,
    N_RIDES,
    COST
  )
    .openingHours(T10_30, T12_30)
    .withTimePenalty(TIME_PENALTY_VALUE);

  static List<TestAccessEgress> parseAccessEgressTestCases() {
    return List.of(OPENING_HOURS);
    //return List.of(WALK, FLEX, FLEX_AND_WALK, FREE, OPENING_HOURS, VIA, TIME_PENALTY, EVERYTHING);
  }

  @ParameterizedTest
  @MethodSource("parseAccessEgressTestCases")
  void parseAccessEgress(TestAccessEgress subject) {
    var toString = subject.toString();
    assertEquals(subject, TestAccessEgress.of(toString), toString);
  }
}
