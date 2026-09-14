package org.opentripplanner.raptor.api.request.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;

class RaptorVisitStopViaConnectionTest {

  public static final int STOP_A = 1;
  public static final int STOP_B = 2;
  public static final int C1 = 100;
  public static final int MINIMUM_WAIT_TIME = 7;

  private final RaptorVisitStopViaConnection a = new RaptorVisitStopViaConnection(
    STOP_A,
    MINIMUM_WAIT_TIME
  );
  private final RaptorVisitStopViaConnection aLessMinWaitTime = new RaptorVisitStopViaConnection(
    STOP_A,
    MINIMUM_WAIT_TIME - 1
  );
  private final RaptorVisitStopViaConnection b = new RaptorVisitStopViaConnection(
    STOP_B,
    MINIMUM_WAIT_TIME
  );
  private final RaptorVisitStopViaConnection sameA = new RaptorVisitStopViaConnection(
    STOP_A,
    MINIMUM_WAIT_TIME
  );

  @Test
  void fromStop() {
    assertEquals(STOP_A, a.fromStop());
  }

  @Test
  void minimumWaitTime() {
    assertEquals(MINIMUM_WAIT_TIME, a.minimumWaitTime());
  }

  @Test
  void leftDominanceExist() {
    assertTrue(a.leftDominanceExist(b));
    assertTrue(b.leftDominanceExist(a));
    assertFalse(a.leftDominanceExist(aLessMinWaitTime));
    assertTrue(aLessMinWaitTime.leftDominanceExist(a));
    assertFalse(a.leftDominanceExist(sameA));
    assertFalse(a.leftDominanceExist(a));
  }

  @Test
  void testEquals() {
    assertFalse(a.equals(b));
    assertFalse(a.equals(aLessMinWaitTime));
    assertTrue(a.equals(sameA));
    assertTrue(a.equals(a));
  }

  @Test
  void testHashCode() {
    assertNotEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a.hashCode(), aLessMinWaitTime.hashCode());
    assertEquals(a.hashCode(), sameA.hashCode());
    assertEquals(a.hashCode(), a.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("(stop 1 [7s])", a.toString());
    assertEquals("(stop A [7s])", a.toString(RaptorTestConstants::stopIndexToName));
  }
}
