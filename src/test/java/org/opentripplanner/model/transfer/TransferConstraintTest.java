package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;

import org.junit.jupiter.api.Test;
import org.opentripplanner.util.time.DurationUtils;

public class TransferConstraintTest implements TransferTestData {

  public static final int MAX_WAIT_TIME_ONE_HOUR = DurationUtils.durationInSeconds("1h");

  private final TransferConstraint NO_CONSTRAINS = TransferConstraint.create().build();
  private final TransferConstraint RECOMMENDED = TransferConstraint.create().recommended().build();
  private final TransferConstraint STAY_SEATED = TransferConstraint.create().staySeated().build();
  private final TransferConstraint GUARANTIED = TransferConstraint.create().guaranteed().build();
  private final TransferConstraint MAX_WAIT_TIME = TransferConstraint.create()
          .guaranteed().maxWaitTime(MAX_WAIT_TIME_ONE_HOUR).build();
  private final TransferConstraint EVERYTHING = TransferConstraint.create()
          .staySeated().guaranteed().preferred().maxWaitTime(MAX_WAIT_TIME_ONE_HOUR).build();

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