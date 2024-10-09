package org.opentripplanner.framework.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.DurationUtils;

class TimeAndCostTest {

  private static final int COST = 45;
  private static final int TIME_IN_SECONDS = DurationUtils.durationInSeconds("12m45s");
  private static final Duration TIME = Duration.ofSeconds(TIME_IN_SECONDS);
  private final TimeAndCost subject = new TimeAndCost(TIME, Cost.costOfSeconds(COST));

  @Test
  void timeInSeconds() {
    assertEquals(TIME_IN_SECONDS, subject.timeInSeconds());
  }

  @Test
  void testToString() {
    assertEquals("(12m45s $45)", subject.toString());
    assertEquals("(0s $0)", TimeAndCost.ZERO.toString());
  }

  @Test
  void time() {
    assertEquals(Duration.ZERO, TimeAndCost.ZERO.time());
    assertEquals(TIME, subject.time());
  }

  @Test
  void isZero() {
    assertTrue(Duration.ZERO.isZero());
    assertFalse(subject.isZero());
  }

  @Test
  void cost() {
    assertEquals(Cost.ZERO, TimeAndCost.ZERO.cost());
    assertEquals(COST, subject.cost().toSeconds());
  }
}
