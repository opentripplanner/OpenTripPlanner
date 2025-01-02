package org.opentripplanner.transit.model.network.grouppriority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriority32n.dominate;
import static org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriority32n.groupId;
import static org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriority32n.mergeInGroupId;

import org.junit.jupiter.api.Test;

class TransitGroupPriority32nTest {

  private static final int GROUP_INDEX_0 = 0;
  private static final int GROUP_INDEX_1 = 1;
  private static final int GROUP_INDEX_2 = 2;
  private static final int GROUP_INDEX_30 = 30;
  private static final int GROUP_INDEX_31 = 31;

  private static final int GROUP_0 = groupId(GROUP_INDEX_0);
  private static final int GROUP_1 = groupId(GROUP_INDEX_1);
  private static final int GROUP_2 = groupId(GROUP_INDEX_2);
  private static final int GROUP_30 = groupId(GROUP_INDEX_30);
  private static final int GROUP_31 = groupId(GROUP_INDEX_31);

  @Test
  void testGroupId() {
    assertEqualsHex(0x00_00_00_00, groupId(0));
    assertEqualsHex(0x00_00_00_01, groupId(1));
    assertEqualsHex(0x00_00_00_02, groupId(2));
    assertEqualsHex(0x00_00_00_04, groupId(3));
    assertEqualsHex(0x40_00_00_00, groupId(31));
    assertEqualsHex(0x80_00_00_00, groupId(32));

    assertThrows(IllegalArgumentException.class, () -> groupId(-1));
    assertThrows(IllegalArgumentException.class, () -> groupId(33));
  }

  @Test
  void mergeTransitGroupPriorityIds() {
    assertEqualsHex(GROUP_0, mergeInGroupId(GROUP_0, GROUP_0));
    assertEqualsHex(GROUP_1, mergeInGroupId(GROUP_1, GROUP_1));
    assertEqualsHex(GROUP_0 | GROUP_1, mergeInGroupId(GROUP_0, GROUP_1));
    assertEqualsHex(GROUP_30 | GROUP_31, mergeInGroupId(GROUP_30, GROUP_31));
    assertEqualsHex(
      GROUP_0 | GROUP_1 | GROUP_2 | GROUP_30 | GROUP_31,
      mergeInGroupId(GROUP_0 | GROUP_1 | GROUP_2 | GROUP_30, GROUP_31)
    );
  }

  @Test
  void dominanceFunction() {
    assertFalse(dominate(GROUP_0, GROUP_0));
    assertFalse(dominate(GROUP_31, GROUP_31));
    assertFalse(dominate(GROUP_1 | GROUP_2, GROUP_1 | GROUP_2));

    assertTrue(dominate(GROUP_0, GROUP_1));
    assertFalse(dominate(GROUP_1, GROUP_0));

    assertTrue(dominate(GROUP_1, GROUP_1 | GROUP_2));
    assertFalse(dominate(GROUP_1 | GROUP_2, GROUP_1));
  }

  static void assertEqualsHex(int expected, int actual) {
    assertEquals(expected, actual, "%08x == %08x".formatted(expected, actual));
  }
}
