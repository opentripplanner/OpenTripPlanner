package org.opentripplanner.model.transfer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.model.transfer.TransferConstraint.MAX_WAIT_TIME_NOT_SET;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.PREFERRED;

import org.junit.Test;
import org.opentripplanner.util.time.DurationUtils;

public class TransferConstraintTest implements TransferTestData {

  public static final int MAX_WAIT_TIME_ONE_HOUR = DurationUtils.duration("1h");

  private final TransferConstraint NO_CONSTRAINS = new TransferConstraint(
          ALLOWED, false, false, MAX_WAIT_TIME_NOT_SET
  );
  private final TransferConstraint RECOMMENDED = new TransferConstraint(
          TransferPriority.RECOMMENDED, false, false, MAX_WAIT_TIME_NOT_SET
  );
  private final TransferConstraint STAY_SEATED = new TransferConstraint(
          ALLOWED, true, false, MAX_WAIT_TIME_NOT_SET
  );
  private final TransferConstraint GUARANTIED = new TransferConstraint(
          ALLOWED, false, true, MAX_WAIT_TIME_NOT_SET
  );
  private final TransferConstraint MAX_WAIT_TIME = new TransferConstraint(ALLOWED, false, false, MAX_WAIT_TIME_ONE_HOUR);
  private final TransferConstraint EVERYTHING = new TransferConstraint(PREFERRED, true, true, MAX_WAIT_TIME_ONE_HOUR);

  @Test
  public void getPriority() {
    assertEquals(TransferPriority.RECOMMENDED, RECOMMENDED.getPriority());
    assertEquals(ALLOWED, NO_CONSTRAINS.getPriority());
  }

  @Test
  public void isStaySeated() {
    assertTrue(STAY_SEATED.isStaySeated());
    assertFalse(NO_CONSTRAINS.isStaySeated());
  }

  @Test
  public void isGuaranteed() {
    assertTrue(GUARANTIED.isGuaranteed());
    assertFalse(NO_CONSTRAINS.isGuaranteed());
  }

  @Test
  public void getMaxWaitTime() {
    assertEquals(MAX_WAIT_TIME_ONE_HOUR, MAX_WAIT_TIME.getMaxWaitTime());
  }

  @Test
  public void priorityCost() {
    assertEquals(0, NO_CONSTRAINS.priorityCost());
    assertEquals(-1, RECOMMENDED.priorityCost());
    assertEquals(-10, GUARANTIED.priorityCost());
    assertEquals(-100, STAY_SEATED.priorityCost());
    assertEquals(-112, EVERYTHING.priorityCost());
  }

  @Test
  public void noConstraints() {
    assertTrue(NO_CONSTRAINS.noConstraints());
    assertFalse(STAY_SEATED.noConstraints());
    assertFalse(GUARANTIED.noConstraints());
    assertFalse(RECOMMENDED.noConstraints());
    assertFalse(MAX_WAIT_TIME.noConstraints());
    assertFalse(EVERYTHING.noConstraints());
  }

  @Test
  public void testToString() {
    assertEquals("{ NONE }", NO_CONSTRAINS.toString());
    assertEquals(
            "{priority: PREFERRED, maxWaitTime: 1h, staySeated, guaranteed}",
            EVERYTHING.toString()
    );
  }
}