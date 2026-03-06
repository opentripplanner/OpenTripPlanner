package org.opentripplanner.raptor.moduletests.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.api.request.TestGroupPriorityCalculator.GROUP_A;
import static org.opentripplanner.raptor.api.request.TestGroupPriorityCalculator.GROUP_B;
import static org.opentripplanner.raptor.api.request.TestGroupPriorityCalculator.GROUP_C;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupPriorityCalculator;
import org.opentripplanner.raptor.api.request.TestGroupPriorityCalculator;

/**
 * Note! This is testing a test-fixture, not the Raptor code. This is a prerequesit for other
 * tests using the {@link TestGroupPriorityCalculator}.
 */
public class GroupPriorityCalculatorTest {

  private static final RaptorTransitGroupPriorityCalculator PRIORITY_CALCULATOR =
    new TestGroupPriorityCalculator();

  private static final int GROUP_AB = PRIORITY_CALCULATOR.mergeInGroupId(GROUP_A, GROUP_B);
  private static final int GROUP_AC = PRIORITY_CALCULATOR.mergeInGroupId(GROUP_A, GROUP_C);

  /**
   * Make sure the calculator and group setup is done correct (test-fixture).
   */
  @Test
  void assertGroupCalculatorIsSetupCorrect() {
    var d = PRIORITY_CALCULATOR.dominanceFunction();

    assertTrue(d.leftDominateRight(GROUP_A, GROUP_B));
    assertTrue(d.leftDominateRight(GROUP_B, GROUP_A));
    assertFalse(d.leftDominateRight(GROUP_A, GROUP_A));
    assertTrue(d.leftDominateRight(GROUP_A, GROUP_AB));
    assertFalse(d.leftDominateRight(GROUP_AB, GROUP_A));
    assertFalse(d.leftDominateRight(GROUP_AB, GROUP_AB));
    assertTrue(d.leftDominateRight(GROUP_AB, GROUP_AC));
    assertTrue(d.leftDominateRight(GROUP_AC, GROUP_AB));
  }
}
