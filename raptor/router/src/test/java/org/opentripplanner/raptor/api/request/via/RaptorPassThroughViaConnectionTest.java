package org.opentripplanner.raptor.api.request.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;

class RaptorPassThroughViaConnectionTest {

  public static final int STOP_A = 1;
  public static final int STOP_B = 2;
  private final RaptorPassThroughViaConnection a = new RaptorPassThroughViaConnection(STOP_A);
  private final RaptorPassThroughViaConnection b = new RaptorPassThroughViaConnection(STOP_B);
  private final RaptorPassThroughViaConnection sameAsA = new RaptorPassThroughViaConnection(STOP_A);

  @Test
  void fromStop() {
    assertEquals(STOP_A, a.fromStop());
  }

  @Test
  void leftDominanceExist() {
    assertTrue(a.leftDominanceExist(b));
    assertTrue(b.leftDominanceExist(a));
    assertFalse(a.leftDominanceExist(sameAsA));
  }

  @Test
  void testEquals() {
    assertTrue(a.equals(a));
    assertTrue(a.equals(sameAsA));
    assertFalse(a.equals(b));
  }

  @Test
  void testHashCode() {
    assertEquals(a.hashCode(), sameAsA.hashCode());
    assertNotEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("(stop 1)", a.toString());
    assertEquals("(stop A)", a.toString(RaptorTestConstants::stopIndexToName));
  }
}
