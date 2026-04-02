package org.opentripplanner.astar.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.astar.TestState;

class DurationTerminationStrategyTest {

  private static final DurationTerminationStrategy<TestState> STRATEGY =
    new DurationTerminationStrategy<TestState>(Duration.ofMinutes(10));

  @Test
  void shouldNotTerminateBeforeDuration() {
    assertFalse(STRATEGY.shouldSearchTerminate(new TestState(null, 0, 0)));
    assertFalse(STRATEGY.shouldSearchTerminate(new TestState(null, 0, 300)));
    assertFalse(STRATEGY.shouldSearchTerminate(new TestState(null, 0, 599)));
  }

  @Test
  void shouldTerminateAfterDuration() {
    assertTrue(STRATEGY.shouldSearchTerminate(new TestState(null, 0, 601)));
    assertTrue(STRATEGY.shouldSearchTerminate(new TestState(null, 0, 1000)));
  }
}
