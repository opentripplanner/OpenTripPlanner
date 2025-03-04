package org.opentripplanner.raptor.moduletests.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupPriorityCalculator;

public class TestGroupPriorityCalculator implements RaptorTransitGroupPriorityCalculator {

  public static final RaptorTransitGroupPriorityCalculator PRIORITY_CALCULATOR =
    new TestGroupPriorityCalculator();

  public static final int GROUP_A = 0x01;
  public static final int GROUP_B = 0x02;
  public static final int GROUP_C = 0x04;

  private static final int GROUP_AB = PRIORITY_CALCULATOR.mergeInGroupId(GROUP_A, GROUP_B);
  private static final int GROUP_AC = PRIORITY_CALCULATOR.mergeInGroupId(GROUP_A, GROUP_C);

  @Override
  public int mergeInGroupId(int currentGroupIds, int boardingGroupId) {
    return currentGroupIds | boardingGroupId;
  }

  /**
   * Left dominate right, if right has at least one priority group not in left.
   */
  @Override
  public DominanceFunction dominanceFunction() {
    return (l, r) -> ((l ^ r) & r) != 0;
  }

  /**
   * Make sure the calculator and group setup is done correct.
   */
  @Test
  void assetGroupCalculatorIsSetupCorrect() {
    var d = PRIORITY_CALCULATOR.dominanceFunction();

    assertTrue(d.leftDominateRight(GROUP_A, GROUP_B));
    assertTrue(d.leftDominateRight(GROUP_B, GROUP_A));
    assertFalse(d.leftDominateRight(GROUP_A, GROUP_A));
    // 3 = 1&2, 5 = 1&4
    assertTrue(d.leftDominateRight(GROUP_A, GROUP_AB));
    assertFalse(d.leftDominateRight(GROUP_AB, GROUP_A));
    assertFalse(d.leftDominateRight(GROUP_AB, GROUP_AB));
    assertTrue(d.leftDominateRight(GROUP_AB, GROUP_AC));
    assertTrue(d.leftDominateRight(GROUP_AC, GROUP_AB));
  }
}
