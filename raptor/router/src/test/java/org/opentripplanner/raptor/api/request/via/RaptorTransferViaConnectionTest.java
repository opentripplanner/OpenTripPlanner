package org.opentripplanner.raptor.api.request.via;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransfer;

class RaptorTransferViaConnectionTest {

  public static final int STOP_A = 1;
  public static final int STOP_B = 2;
  public static final int STOP_C = 3;
  public static final int DURATION = 20;
  public static final int C1 = 100;
  public static final TestTransfer TRANSFER_B = TestTransfer.transfer(STOP_B, DURATION, C1);
  public static final TestTransfer TRANSFER_C = TestTransfer.transfer(STOP_C, DURATION, C1);
  public static final int MINIMUM_WAIT_TIME = 7;

  private final RaptorTransferViaConnection a2b = new RaptorTransferViaConnection(
    STOP_A,
    MINIMUM_WAIT_TIME,
    TRANSFER_B
  );
  private final RaptorTransferViaConnection a2c = new RaptorTransferViaConnection(
    STOP_A,
    MINIMUM_WAIT_TIME,
    TRANSFER_C
  );
  private final RaptorTransferViaConnection a2bLessMinWaitTime = new RaptorTransferViaConnection(
    STOP_A,
    MINIMUM_WAIT_TIME - 1,
    TRANSFER_B
  );
  private final RaptorTransferViaConnection c2b = new RaptorTransferViaConnection(
    STOP_C,
    MINIMUM_WAIT_TIME,
    TRANSFER_B
  );
  private final RaptorTransferViaConnection sameA2b = new RaptorTransferViaConnection(
    STOP_A,
    MINIMUM_WAIT_TIME,
    TRANSFER_B
  );

  @Test
  void transfer() {
    assertSame(TRANSFER_B, a2b.transfer());
  }

  @Test
  void fromStop() {
    assertEquals(STOP_A, a2b.fromStop());
  }

  @Test
  void toStop() {
    assertEquals(STOP_B, a2b.toStop());
  }

  @Test
  void durationInSeconds() {
    assertEquals(DURATION + MINIMUM_WAIT_TIME, a2b.durationInSeconds());
  }

  @Test
  void c1() {
    assertEquals(C1, a2b.c1());
  }

  @Test
  void leftDominanceExist() {
    assertTrue(a2b.leftDominanceExist(a2c));
    assertTrue(a2c.leftDominanceExist(a2b));
    assertTrue(a2b.leftDominanceExist(c2b));
    assertTrue(c2b.leftDominanceExist(a2b));
    assertTrue(a2bLessMinWaitTime.leftDominanceExist(a2b));
    assertFalse(a2b.leftDominanceExist(a2bLessMinWaitTime));
    assertFalse(a2b.leftDominanceExist(sameA2b));
    assertFalse(a2b.leftDominanceExist(a2b));
  }

  @Test
  void testEquals() {
    assertTrue(a2b.equals(a2b));
    assertTrue(a2b.equals(sameA2b));
    assertFalse(a2b.equals(a2c));
    assertFalse(a2c.equals(a2b));
    assertFalse(a2b.equals(c2b));
    assertFalse(c2b.equals(a2b));
    assertFalse(a2b.equals(a2bLessMinWaitTime));
    assertFalse(a2bLessMinWaitTime.equals(a2b));
  }

  @Test
  void testHashCode() {
    assertEquals(a2b.hashCode(), a2b.hashCode());
    assertEquals(a2b.hashCode(), sameA2b.hashCode());
    assertNotEquals(a2b.hashCode(), a2c.hashCode());
    assertNotEquals(a2b.hashCode(), a2bLessMinWaitTime.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("(transfer 1 ~ 2 [27s C₁1])", a2b.toString());
    assertEquals("(transfer A ~ B [27s C₁1])", a2b.toString(RaptorTestConstants::stopIndexToName));
  }
}
