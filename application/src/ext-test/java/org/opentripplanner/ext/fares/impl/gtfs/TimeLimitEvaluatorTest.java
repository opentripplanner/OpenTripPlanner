package org.opentripplanner.ext.fares.impl.gtfs;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.fares.model.TimeLimitType.ARRIVAL_TO_ARRIVAL;
import static org.opentripplanner.ext.fares.model.TimeLimitType.ARRIVAL_TO_DEPARTURE;
import static org.opentripplanner.ext.fares.model.TimeLimitType.DEPARTURE_TO_ARRIVAL;
import static org.opentripplanner.ext.fares.model.TimeLimitType.DEPARTURE_TO_DEPARTURE;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.fares.model.TimeLimit;
import org.opentripplanner.ext.fares.model.TimeLimitType;
import org.opentripplanner.model.plan.TestTransitLeg;
import org.opentripplanner.model.plan.TransitLeg;

class TimeLimitEvaluatorTest {

  private static final TransitLeg FIRST = TestTransitLeg.of()
    .withStartTime("10:00")
    .withEndTime("10:05")
    .build();
  private static final TransitLeg SECOND = TestTransitLeg.of()
    .withStartTime("10:10")
    .withEndTime("10:20")
    .build();

  private static List<Arguments> withinLimitCases() {
    return List.of(
      Arguments.of(DEPARTURE_TO_ARRIVAL, ofMinutes(20)),
      Arguments.of(DEPARTURE_TO_ARRIVAL, ofHours(1)),
      Arguments.of(DEPARTURE_TO_DEPARTURE, ofMinutes(11)),
      Arguments.of(DEPARTURE_TO_DEPARTURE, ofHours(1)),
      Arguments.of(ARRIVAL_TO_ARRIVAL, ofMinutes(16)),
      Arguments.of(ARRIVAL_TO_DEPARTURE, ofMinutes(5)),
      Arguments.of(ARRIVAL_TO_DEPARTURE, ofHours(1))
    );
  }

  @ParameterizedTest
  @MethodSource("withinLimitCases")
  void withinLimit(TimeLimitType type, Duration duration) {
    var limit = new TimeLimit(type, duration);
    assertTrue(TimeLimitEvaluator.withinTimeLimit(limit, FIRST, SECOND));
  }

  private static List<Arguments> outsideLimitCases() {
    return List.of(
      Arguments.of(DEPARTURE_TO_ARRIVAL, ofMinutes(20).minusSeconds(20)),
      Arguments.of(DEPARTURE_TO_DEPARTURE, ofMinutes(10).minusSeconds(1)),
      Arguments.of(ARRIVAL_TO_ARRIVAL, ofMinutes(15).minusSeconds(1)),
      Arguments.of(ARRIVAL_TO_DEPARTURE, ofMinutes(5).minusSeconds(1))
    );
  }

  @ParameterizedTest
  @MethodSource("outsideLimitCases")
  void outsideLimit(TimeLimitType type, Duration duration) {
    var limit = new TimeLimit(type, duration);
    assertFalse(TimeLimitEvaluator.withinTimeLimit(limit, FIRST, SECOND));
  }
}
