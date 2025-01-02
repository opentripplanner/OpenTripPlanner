package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StopPriorityCostCalculatorTest {

  @Test
  void extraStopPriorityCost() {
    var subject = new StopPriorityCostCalculator(3.775, new int[] { 0, 1, 20, 100 });

    assertEquals(0, subject.extraStopPriorityCost(0));
    assertEquals(4, subject.extraStopPriorityCost(1));
    assertEquals(76, subject.extraStopPriorityCost(2));
    assertEquals(378, subject.extraStopPriorityCost(3));
  }
}
