package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.grouppriority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupCalculator;

class TransitGroupPriority32nTest {

  private static final int GROUP_INDEX_0 = 0;
  private static final int GROUP_INDEX_1 = 1;
  private static final int GROUP_INDEX_2 = 2;
  private static final int GROUP_INDEX_30 = 30;
  private static final int GROUP_INDEX_31 = 31;

  private static final int GROUP_0 = TransitGroupPriority32n.groupId(GROUP_INDEX_0);
  private static final int GROUP_1 = TransitGroupPriority32n.groupId(GROUP_INDEX_1);
  private static final int GROUP_2 = TransitGroupPriority32n.groupId(GROUP_INDEX_2);
  private static final int GROUP_30 = TransitGroupPriority32n.groupId(GROUP_INDEX_30);
  private static final int GROUP_31 = TransitGroupPriority32n.groupId(GROUP_INDEX_31);
  private static final RaptorTransitGroupCalculator subjct = TransitGroupPriority32n.priorityCalculator();

  @Test
  void groupId() {
    assertEqualsHex(0x00_00_00_00, TransitGroupPriority32n.groupId(0));
    assertEqualsHex(0x00_00_00_01, TransitGroupPriority32n.groupId(1));
    assertEqualsHex(0x00_00_00_02, TransitGroupPriority32n.groupId(2));
    assertEqualsHex(0x00_00_00_04, TransitGroupPriority32n.groupId(3));
    assertEqualsHex(0x40_00_00_00, TransitGroupPriority32n.groupId(31));
    assertEqualsHex(0x80_00_00_00, TransitGroupPriority32n.groupId(32));

    assertThrows(IllegalArgumentException.class, () -> TransitGroupPriority32n.groupId(-1));
    assertThrows(IllegalArgumentException.class, () -> TransitGroupPriority32n.groupId(33));
  }

  @Test
  void mergeTransitGroupPriorityIds() {
    assertEqualsHex(GROUP_0, subjct.mergeGroupIds(GROUP_0, GROUP_0));
    assertEqualsHex(GROUP_1, subjct.mergeGroupIds(GROUP_1, GROUP_1));
    assertEqualsHex(GROUP_0 | GROUP_1, subjct.mergeGroupIds(GROUP_0, GROUP_1));
    assertEqualsHex(GROUP_30 | GROUP_31, subjct.mergeGroupIds(GROUP_30, GROUP_31));
    assertEqualsHex(
      GROUP_0 | GROUP_1 | GROUP_2 | GROUP_30 | GROUP_31,
      subjct.mergeGroupIds(GROUP_0 | GROUP_1 | GROUP_2 | GROUP_30, GROUP_31)
    );
  }

  @Test
  void dominanceFunction() {
    assertFalse(subjct.dominanceFunction().leftDominateRight(GROUP_0, GROUP_0));
    assertFalse(subjct.dominanceFunction().leftDominateRight(GROUP_31, GROUP_31));
    assertFalse(subjct.dominanceFunction().leftDominateRight(GROUP_1 | GROUP_2, GROUP_1 | GROUP_2));

    assertTrue(subjct.dominanceFunction().leftDominateRight(GROUP_0, GROUP_1));
    assertFalse(subjct.dominanceFunction().leftDominateRight(GROUP_1, GROUP_0));

    assertTrue(subjct.dominanceFunction().leftDominateRight(GROUP_1, GROUP_1 | GROUP_2));
    assertFalse(subjct.dominanceFunction().leftDominateRight(GROUP_1 | GROUP_2, GROUP_1));
  }

  static void assertEqualsHex(int expected, int actual) {
    assertEquals(expected, actual, "%08x == %08x".formatted(expected, actual));
  }
}
