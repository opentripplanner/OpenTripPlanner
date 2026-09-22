package org.opentripplanner.raptor.api.request;

import org.opentripplanner.raptor.api.model.DominanceFunction;

/**
 * A dummy implementation of the {@link RaptorTransitGroupPriorityCalculator} interface to
 * allow unit test priority group features in Raptor.
 */
public class TestGroupPriorityCalculator implements RaptorTransitGroupPriorityCalculator {

  public static final int GROUP_A = 0x01;
  public static final int GROUP_B = 0x02;
  public static final int GROUP_C = 0x04;

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
}
