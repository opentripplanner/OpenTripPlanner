package org.opentripplanner.model.transfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.application.OTPFeature.MinimumTransferTimeIsDefinitive;
import static org.opentripplanner.model.transfer.TransferPriority.ALLOWED;
import static org.opentripplanner.raptor.api.model.SearchDirection.FORWARD;
import static org.opentripplanner.raptor.api.model.SearchDirection.REVERSE;

import java.util.function.IntSupplier;
import org.junit.jupiter.api.Test;
import org.opentripplanner.utils.time.DurationUtils;

public class TransferConstraintTest {

  private final int MAX_WAIT_TIME_ONE_HOUR = DurationUtils.durationInSeconds("1h");
  private final int D3m = DurationUtils.durationInSeconds("3m");

  private final TransferConstraint NO_CONSTRAINS = TransferConstraint.of().build();
  private final TransferConstraint RECOMMENDED = TransferConstraint.of().recommended().build();
  private final TransferConstraint STAY_SEATED = TransferConstraint.of().staySeated().build();
  private final TransferConstraint GUARANTEED = TransferConstraint.of().guaranteed().build();
  private final TransferConstraint NOT_ALLOWED = TransferConstraint.of().notAllowed().build();
  private final TransferConstraint MAX_WAIT_TIME = TransferConstraint.of()
    .guaranteed()
    .maxWaitTime(MAX_WAIT_TIME_ONE_HOUR)
    .build();
  private final TransferConstraint EVERYTHING = TransferConstraint.of()
    .staySeated()
    .guaranteed()
    .preferred()
    .maxWaitTime(MAX_WAIT_TIME_ONE_HOUR)
    .build();
  private final TransferConstraint MIN_TX_TIME = TransferConstraint.of()
    .minTransferTime(D3m)
    .build();

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
    assertTrue(GUARANTEED.isGuaranteed());
    assertFalse(NO_CONSTRAINS.isGuaranteed());
  }

  @Test
  public void isFacilitated() {
    assertTrue(GUARANTEED.isFacilitated());
    assertTrue(STAY_SEATED.isFacilitated());
    assertFalse(NO_CONSTRAINS.isFacilitated());
    assertFalse(NOT_ALLOWED.isFacilitated());
    assertFalse(MIN_TX_TIME.isFacilitated());
  }

  @Test
  public void useInRaptorRouting() {
    assertTrue(GUARANTEED.includeInRaptorRouting());
    assertTrue(STAY_SEATED.includeInRaptorRouting());
    assertFalse(NO_CONSTRAINS.includeInRaptorRouting());
    assertTrue(NOT_ALLOWED.includeInRaptorRouting());
    assertTrue(MIN_TX_TIME.includeInRaptorRouting());
  }

  @Test
  public void isNotAllowed() {
    assertTrue(NOT_ALLOWED.isNotAllowed());
    assertFalse(GUARANTEED.isNotAllowed());
    assertFalse(NO_CONSTRAINS.isNotAllowed());
    assertFalse(MIN_TX_TIME.isNotAllowed());
  }

  @Test
  public void getMaxWaitTime() {
    assertEquals(MAX_WAIT_TIME_ONE_HOUR, MAX_WAIT_TIME.getMaxWaitTime());
  }

  @Test
  public void getMinTransferTime() {
    assertTrue(MIN_TX_TIME.isMinTransferTimeSet());
    assertEquals(D3m, MIN_TX_TIME.getMinTransferTime());
  }

  @Test
  public void cost() {
    assertEquals(33_00, NO_CONSTRAINS.cost());
    assertEquals(33_00, MIN_TX_TIME.cost());
    assertEquals(32_00, RECOMMENDED.cost());
    assertEquals(23_00, GUARANTEED.cost());
    assertEquals(13_00, STAY_SEATED.cost());
    assertEquals(11_00, EVERYTHING.cost());
  }

  @Test
  public void noConstraints() {
    assertTrue(NO_CONSTRAINS.isRegularTransfer());
    assertFalse(STAY_SEATED.isRegularTransfer());
    assertFalse(GUARANTEED.isRegularTransfer());
    assertFalse(RECOMMENDED.isRegularTransfer());
    assertFalse(MAX_WAIT_TIME.isRegularTransfer());
    assertFalse(EVERYTHING.isRegularTransfer());
  }

  @Test
  public void testToString() {
    assertEquals("(no constraints)", NO_CONSTRAINS.toString());
    assertEquals(
      "(priority: PREFERRED, staySeated, guaranteed, maxWaitTime: 1h)",
      EVERYTHING.toString()
    );
  }

  @Test
  public void staticPriorityCost() {
    assertEquals(NO_CONSTRAINS.cost(), TransferConstraint.cost(null));
    assertEquals(NO_CONSTRAINS.cost(), TransferConstraint.cost(NO_CONSTRAINS));
    assertEquals(GUARANTEED.cost(), TransferConstraint.cost(GUARANTEED));
  }

  @Test
  public void calculateConstrainedTransferTargetTimeForwardSearch() {
    // source-arrival-time
    int t0 = 300;
    // Extra user transfer-time
    int dt = 240;
    // Expected arrival-time with minTransferTime constrained transfer
    int expAt = t0 + dt + MIN_TX_TIME.getMinTransferTime();
    // Regular transfer with arrival time 1 second before MIN_TRANSFER_TIME constrained transfer
    int txMinus = expAt - 1;
    IntSupplier txMinusOp = () -> txMinus;
    // Regular transfer with arrival time 1 second after MIN_TRANSFER_TIME constrained transfer
    int txPlus = expAt + 1;
    IntSupplier txPlusOp = () -> txPlus;

    assertEquals(txMinus, NO_CONSTRAINS.calculateTransferTargetTime(t0, dt, txMinusOp, FORWARD));
    assertEquals(txMinus, RECOMMENDED.calculateTransferTargetTime(t0, dt, txMinusOp, FORWARD));
    assertEquals(t0, GUARANTEED.calculateTransferTargetTime(t0, dt, txMinusOp, FORWARD));
    assertEquals(t0, STAY_SEATED.calculateTransferTargetTime(t0, dt, txMinusOp, FORWARD));
    assertEquals(t0, EVERYTHING.calculateTransferTargetTime(t0, dt, txMinusOp, FORWARD));

    MinimumTransferTimeIsDefinitive.testOn(() -> {
      assertEquals(expAt, MIN_TX_TIME.calculateTransferTargetTime(t0, dt, txMinusOp, FORWARD));
      assertEquals(expAt, MIN_TX_TIME.calculateTransferTargetTime(t0, dt, txPlusOp, FORWARD));
    });
    MinimumTransferTimeIsDefinitive.testOff(() -> {
      assertEquals(expAt, MIN_TX_TIME.calculateTransferTargetTime(t0, dt, txMinusOp, FORWARD));
      assertEquals(txPlus, MIN_TX_TIME.calculateTransferTargetTime(t0, dt, txPlusOp, FORWARD));
    });
  }

  @Test
  public void calculateConstrainedTransferTargetTimeReverseSearch() {
    // source-arrival-time
    int t0 = 300;
    // Extra user transfer-time
    int dt = 240;
    // Expected arrival-time with minTransferTime constrained transfer
    int expAt = t0 - (dt + MIN_TX_TIME.getMinTransferTime());
    // Regular transfer with arrival time 1 second before MIN_TRANSFER_TIME constrained transfer
    int txMinus = expAt + 1;
    IntSupplier txMinusOp = () -> txMinus;
    // Regular transfer with arrival time 1 second after MIN_TRANSFER_TIME constrained transfer
    int txPlus = expAt - 1;
    IntSupplier txPlusOp = () -> txPlus;

    assertEquals(txMinus, NO_CONSTRAINS.calculateTransferTargetTime(t0, dt, txMinusOp, REVERSE));
    assertEquals(txMinus, RECOMMENDED.calculateTransferTargetTime(t0, dt, txMinusOp, REVERSE));
    assertEquals(t0, GUARANTEED.calculateTransferTargetTime(t0, dt, txMinusOp, REVERSE));
    assertEquals(t0, STAY_SEATED.calculateTransferTargetTime(t0, dt, txMinusOp, REVERSE));
    assertEquals(t0, EVERYTHING.calculateTransferTargetTime(t0, dt, txMinusOp, REVERSE));

    MinimumTransferTimeIsDefinitive.testOn(() -> {
      assertEquals(expAt, MIN_TX_TIME.calculateTransferTargetTime(t0, dt, txMinusOp, REVERSE));
      assertEquals(expAt, MIN_TX_TIME.calculateTransferTargetTime(t0, dt, txPlusOp, REVERSE));
    });
    MinimumTransferTimeIsDefinitive.testOff(() -> {
      assertEquals(expAt, MIN_TX_TIME.calculateTransferTargetTime(t0, dt, txMinusOp, REVERSE));
      assertEquals(txPlus, MIN_TX_TIME.calculateTransferTargetTime(t0, dt, txPlusOp, REVERSE));
    });
  }
}
