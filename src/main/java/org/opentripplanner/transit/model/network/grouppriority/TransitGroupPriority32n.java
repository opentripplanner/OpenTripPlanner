package org.opentripplanner.transit.model.network.grouppriority;

/**
 * This is a "BitSet" implementation for groupId. It can store up to 31 groups,
 * a set with few elements does NOT dominate a set with more elements.
 */
class TransitGroupPriority32n {

  private static final int GROUP_ZERO = 0;
  private static final int MIN_SEQ_NO = 0;
  private static final int MAX_SEQ_NO = 32;

  /**
   * Left dominates right:
   *   - if right contains a group which does not exist in the left.
   * Left do NOT dominate right:
   *   - if they are equals or
   *   - left is a superset of right.
   */
  static boolean dominate(int left, int right) {
    return ((left ^ right) & right) != 0;
  }

  /**
   * Use this method to map from a continuous group index [0..32) to the groupId used
   * during routing. The ID is implementation-specific and optimized for performance.
   */
  static int groupId(final int priorityGroupIndex) {
    assertValidGroupSeqNo(priorityGroupIndex);
    return priorityGroupIndex == MIN_SEQ_NO ? GROUP_ZERO : 0x01 << (priorityGroupIndex - 1);
  }

  /**
   * Merge a groupId into a set of groupIds.
   */
  static int mergeInGroupId(final int currentSetOfGroupIds, final int newGroupId) {
    return currentSetOfGroupIds | newGroupId;
  }

  @Override
  public String toString() {
    return "TransitGroupPriority32n{}";
  }

  private static void assertValidGroupSeqNo(int priorityGroupIndex) {
    if (priorityGroupIndex < MIN_SEQ_NO) {
      throw new IllegalArgumentException(
        "Transit group priority can not be a negative number: " + priorityGroupIndex
      );
    }
    if (priorityGroupIndex > MAX_SEQ_NO) {
      throw new IllegalArgumentException(
        "Transit group priority exceeds max number of groups: " +
        priorityGroupIndex +
        " (MAX=" +
        MAX_SEQ_NO +
        ")"
      );
    }
  }
}
