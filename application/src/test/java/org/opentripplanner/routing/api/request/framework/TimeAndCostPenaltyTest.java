package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.utils.time.DurationUtils;

class TimeAndCostPenaltyTest {

  private static final double COST_FACTOR = 2.5;
  private static final double TIME_COEFFICIENT = 1.6;
  private static final Duration D55s = Duration.ofSeconds(55);
  public static final TimePenalty TIME_PENALTY = TimePenalty.of(D55s, TIME_COEFFICIENT);

  private final TimeAndCostPenalty subject = new TimeAndCostPenalty(TIME_PENALTY, COST_FACTOR);

  @Test
  void of() {
    assertEquals(TimeAndCostPenalty.of("55s + 1.6t", COST_FACTOR), subject);
  }

  @Test
  void calculate() {
    int inputSeconds = 1000;
    var inputTime = Duration.ofSeconds(inputSeconds);
    var expectedTimeAndCost = new TimeAndCost(
      DurationUtils.duration("27m35s"),
      Cost.costOfCentiSeconds(413750)
    );

    assertEquals(expectedTimeAndCost, subject.calculate(inputTime));
    assertEquals(expectedTimeAndCost, subject.calculate(inputSeconds));
  }

  @Test
  void isEmpty() {
    assertTrue(TimeAndCostPenalty.ZERO.isEmpty());
    assertFalse(subject.isEmpty());
  }

  @Test
  void testToString() {
    assertEquals("(timePenalty: 55s + 1.60 t, costFactor: 2.5)", subject.toString());
  }

  @Test
  void timePenalty() {
    assertEquals(TIME_PENALTY, subject.timePenalty());
  }

  @Test
  void costFactor() {
    assertEquals(COST_FACTOR, subject.costFactor());
  }
}
