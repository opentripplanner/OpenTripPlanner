package org.opentripplanner.transfer.constrained.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.transfer.constrained.model.TransferPriority.ALLOWED;
import static org.opentripplanner.transfer.constrained.model.TransferPriority.NOT_ALLOWED;
import static org.opentripplanner.transfer.constrained.model.TransferPriority.PREFERRED;
import static org.opentripplanner.transfer.constrained.model.TransferPriority.RECOMMENDED;

import org.junit.jupiter.api.Test;

public class TransferPriorityTest {

  @Test
  public void cost() {
    assertEquals(1_00, PREFERRED.cost());
    assertEquals(2_00, RECOMMENDED.cost());
    assertEquals(3_00, ALLOWED.cost());
    assertEquals(1000_00, NOT_ALLOWED.cost());
  }

  @Test
  public void isConstrained() {
    assertTrue(PREFERRED.isConstrained());
    assertTrue(RECOMMENDED.isConstrained());
    assertFalse(ALLOWED.isConstrained());
    assertTrue(NOT_ALLOWED.isConstrained());
  }
}
