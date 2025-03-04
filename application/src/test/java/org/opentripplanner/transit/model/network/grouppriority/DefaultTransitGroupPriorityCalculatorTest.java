package org.opentripplanner.transit.model.network.grouppriority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DefaultTransitGroupPriorityCalculatorTest {

  private final DefaultTransitGroupPriorityCalculator subject =
    new DefaultTransitGroupPriorityCalculator();

  @Test
  void mergeGroupIds() {
    // Smoke test, should not fail
    subject.mergeInGroupId(1, 2);
  }

  @Test
  void dominanceFunction() {
    // This is assuming 1 & 2 represent different transit-groups - this just a smoke test to
    // see that the delegation works as expected. The 'leftDominateRight' is unit-tested elsewhere.
    assertTrue(subject.dominanceFunction().leftDominateRight(1, 2));
    assertTrue(subject.dominanceFunction().leftDominateRight(2, 1));
    assertFalse(subject.dominanceFunction().leftDominateRight(1, 1));
  }

  @Test
  void testToString() {
    assertEquals("DefaultTransitGroupCalculator{Using TGP32n}", subject.toString());
  }
}
