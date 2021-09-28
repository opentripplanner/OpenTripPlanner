package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.transfer.TransferConstraint.MAX_WAIT_TIME_NOT_SET;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.model.transfer.TransferPriority.PREFERRED;

import org.junit.jupiter.api.Test;
import org.opentripplanner.util.time.DurationUtils;

public class TransferConstraintTest implements TransferTestData {

  public static final int MAX_WAIT_TIME_ONE_HOUR = DurationUtils.duration("1h");

  private final TransferConstraint NO_CONSTRAINS = new TransferConstraint(ALLOWED, false, false, MAX_WAIT_TIME_NOT_SET);
  private final TransferConstraint RECOMMENDED = new TransferConstraint(TransferPriority.RECOMMENDED, false, false, MAX_WAIT_TIME_NOT_SET);
  private final TransferConstraint STAY_SEATED = new TransferConstraint(ALLOWED, true, false, MAX_WAIT_TIME_NOT_SET);
  private final TransferConstraint GUARANTIED = new TransferConstraint(ALLOWED, false, true, MAX_WAIT_TIME_NOT_SET);
  private final TransferConstraint MAX_WAIT_TIME = new TransferConstraint(ALLOWED, false, true, MAX_WAIT_TIME_ONE_HOUR);
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
  public void isFacilitated() {
    assertTrue(GUARANTIED.isFacilitated());
    assertTrue(STAY_SEATED.isFacilitated());
    assertFalse(NO_CONSTRAINS.isFacilitated());
  }

  @Test
  public void getMaxWaitTime() {
    assertEquals(MAX_WAIT_TIME_ONE_HOUR, MAX_WAIT_TIME.getMaxWaitTime());
  }

  @Test
  public void cost() {
    assertEquals(33_00, NO_CONSTRAINS.cost());
    assertEquals(32_00, RECOMMENDED.cost());
    assertEquals(23_00, GUARANTIED.cost());
    assertEquals(13_00, STAY_SEATED.cost());
    assertEquals(11_00, EVERYTHING.cost());
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
    assertEquals("{no constraints}", NO_CONSTRAINS.toString());
    assertEquals(
            "{priority: PREFERRED, staySeated, guaranteed, maxWaitTime: 1h}",
            EVERYTHING.toString()
    );
  }

  @Test
  public void staticPriorityCost() {
    assertEquals(NO_CONSTRAINS.cost(), TransferConstraint.cost(null));
    assertEquals(NO_CONSTRAINS.cost(), TransferConstraint.cost(NO_CONSTRAINS));
    assertEquals(GUARANTIED.cost(), TransferConstraint.cost(GUARANTIED));
  }
}