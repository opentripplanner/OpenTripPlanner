package org.opentripplanner.astar.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaxCountTerminationStrategyTest {

  @Test
  void countStates() {
    var countAllStatesStrategy = new MaxCountTerminationStrategy<>(3, state -> true);

    assertFalse(countAllStatesStrategy.shouldSearchTerminate(null));
    assertFalse(countAllStatesStrategy.shouldSearchTerminate(null));
    assertTrue(countAllStatesStrategy.shouldSearchTerminate(null));
    assertTrue(countAllStatesStrategy.shouldSearchTerminate(null));
  }

  @Test
  void countNoStates() {
    var countNoStatesStrategy = new MaxCountTerminationStrategy<>(1, state -> false);

    assertFalse(countNoStatesStrategy.shouldSearchTerminate(null));
    assertFalse(countNoStatesStrategy.shouldSearchTerminate(null));
    assertFalse(countNoStatesStrategy.shouldSearchTerminate(null));
    assertFalse(countNoStatesStrategy.shouldSearchTerminate(null));
  }
}
