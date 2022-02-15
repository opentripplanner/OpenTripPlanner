package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;

import org.junit.jupiter.api.Test;
import org.opentripplanner.util.time.DurationUtils;

public class TransferConstraintTest {

  public static final int MAX_WAIT_TIME_ONE_HOUR = DurationUtils.durationInSeconds("1h");

  private final TransferConstraint NO_CONSTRAINS = TransferConstraint.create().build();
  private final TransferConstraint RECOMMENDED = TransferConstraint.create().recommended().build();
  private final TransferConstraint STAY_SEATED = TransferConstraint.create().staySeated().build();
  private final TransferConstraint GUARANTIED = TransferConstraint.create().guaranteed().build();
  private final TransferConstraint NOT_ALLOWED = TransferConstraint.create().notAllowed().build();
  private final TransferConstraint MAX_WAIT_TIME = TransferConstraint.create()
          .guaranteed().maxWaitTime(MAX_WAIT_TIME_ONE_HOUR).build();
  private final TransferConstraint EVERYTHING = TransferConstraint.create()
          .staySeated().guaranteed().preferred().maxWaitTime(MAX_WAIT_TIME_ONE_HOUR).build();
  private final TransferConstraint MIN_TRANSFER_TIME = TransferConstraint.create()
          .minTransferTime(600).build();

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
    assertFalse(NOT_ALLOWED.isFacilitated());
    assertFalse(MIN_TRANSFER_TIME.isFacilitated());
  }

  @Test
  public void useInRaptorRouting() {
    assertTrue(GUARANTIED.includeInRaptorRouting());
    assertTrue(STAY_SEATED.includeInRaptorRouting());
    assertFalse(NO_CONSTRAINS.includeInRaptorRouting());
    assertTrue(NOT_ALLOWED.includeInRaptorRouting());
    assertTrue(MIN_TRANSFER_TIME.includeInRaptorRouting());
  }

  @Test
  public void isNotAllowed() {
    assertTrue(NOT_ALLOWED.isNotAllowed());
    assertFalse(GUARANTIED.isNotAllowed());
    assertFalse(NO_CONSTRAINS.isNotAllowed());
    assertFalse(MIN_TRANSFER_TIME.isNotAllowed());
  }

  @Test
  public void getMaxWaitTime() {
    assertEquals(MAX_WAIT_TIME_ONE_HOUR, MAX_WAIT_TIME.getMaxWaitTime());
  }

  @Test
  public void getMinTransferTime() {
    assertTrue(MIN_TRANSFER_TIME.isMinTransferTimeSet());
    assertEquals(600, MIN_TRANSFER_TIME.getMinTransferTime());
  }

  @Test
  public void cost() {
    assertEquals(33_00, NO_CONSTRAINS.cost());
    assertEquals(33_00, MIN_TRANSFER_TIME.cost());
    assertEquals(32_00, RECOMMENDED.cost());
    assertEquals(23_00, GUARANTIED.cost());
    assertEquals(13_00, STAY_SEATED.cost());
    assertEquals(11_00, EVERYTHING.cost());
  }

  @Test
  public void noConstraints() {
    assertTrue(NO_CONSTRAINS.isRegularTransfer());
    assertFalse(STAY_SEATED.isRegularTransfer());
    assertFalse(GUARANTIED.isRegularTransfer());
    assertFalse(RECOMMENDED.isRegularTransfer());
    assertFalse(MAX_WAIT_TIME.isRegularTransfer());
    assertFalse(EVERYTHING.isRegularTransfer());
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