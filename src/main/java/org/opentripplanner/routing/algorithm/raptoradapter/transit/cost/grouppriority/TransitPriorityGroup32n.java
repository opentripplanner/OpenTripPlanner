package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.grouppriority;

import org.opentripplanner.raptor.api.model.DominanceFunction;
import org.opentripplanner.raptor.api.request.RaptorTransitPriorityGroupCalculator;

/**
 * This is a "BitSet" implementation for groupId. It can store upto 32 groups,
 * a set with few elements does NOT dominate a set with more elements.
 */
public class TransitPriorityGroup32n {

  private static final int GROUP_ZERO = 0;
  private static final int MIN_SEQ_NO = 0;
  private static final int MAX_SEQ_NO = 32;

  public static RaptorTransitPriorityGroupCalculator priorityCalculator() {
    return new RaptorTransitPriorityGroupCalculator() {
      @Override
      public int mergeTransitPriorityGroupIds(int currentGroupIds, int boardingGroupId) {
        return mergeInGroupId(currentGroupIds, boardingGroupId);
      }

      @Override
      public DominanceFunction dominanceFunction() {
        return TransitPriorityGroup32n::dominate;
      }

      @Override
      public String toString() {
        return "TransitPriorityGroup32nCalculator{}";
      }
    };
  }

  /**
   * Left dominate right, if right contains a group which does not exist in left. Left
   * do NOT dominate right if they are equals or left is a super set of right.
   */
  public static boolean dominate(int left, int right) {
    return ((left ^ right) & right) != 0;
  }

  @Override
  public String toString() {
    return "TransitPriorityGroup32n{}";
  }

  /**
   * Use this method to map from a continuous group index [0..32) to the groupId used
   * during routing. The ID is implementation specific and optimized for performance.
   */
  public static int groupId(final int priorityGroupIndex) {
    assertValidGroupSeqNo(priorityGroupIndex);
    return priorityGroupIndex == MIN_SEQ_NO ? GROUP_ZERO : 0x01 << (priorityGroupIndex - 1);
  }

  /**
   * Merge a groupId into a set of groupIds.
   */
  public static int mergeInGroupId(final int currentSetOfGroupIds, final int newGroupId) {
    return currentSetOfGroupIds | newGroupId;
  }

  private static void assertValidGroupSeqNo(int priorityGroupIndex) {
    if (priorityGroupIndex < MIN_SEQ_NO) {
      throw new IllegalArgumentException(
        "Transit priority group can not be a negative number: " + priorityGroupIndex
      );
    }
    if (priorityGroupIndex > MAX_SEQ_NO) {
      throw new IllegalArgumentException(
        "Transit priority group exceeds max number of groups: " +
        priorityGroupIndex +
        " (MAX=" +
        MAX_SEQ_NO +
        ")"
      );
    }
  }
}
