package org.opentripplanner.raptor.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.raptor.api.model.RaptorValueType.C1;
import static org.opentripplanner.raptor.api.model.RaptorValueType.C2;
import static org.opentripplanner.raptor.api.model.RaptorValueType.RIDES;
import static org.opentripplanner.raptor.api.model.RaptorValueType.TRANSFERS;
import static org.opentripplanner.raptor.api.model.RaptorValueType.TRANSFER_PRIORITY;
import static org.opentripplanner.raptor.api.model.RaptorValueType.WAIT_TIME_COST;

import org.junit.jupiter.api.Test;

class RaptorValueFormatterTest {

  @Test
  void formatC1() {
    assertEquals("C₁0.01", C1.format(1));
    assertEquals("C₁1", C1.format(100));
  }

  @Test
  void formatC2() {
    assertEquals("C₂1", C2.format(1));
  }

  @Test
  void formatWaitTimeCost() {
    assertEquals("Wₜ0.01", WAIT_TIME_COST.format(1));
    assertEquals("Wₜ1", WAIT_TIME_COST.format(100));
  }

  @Test
  void formatNumOfTransfers() {
    assertEquals("Tₙ1", TRANSFERS.format(1));
  }

  @Test
  void formatNumOfRides() {
    assertEquals("Rₙ1", RIDES.format(1));
  }

  @Test
  void formatTransferPriority() {
    assertEquals("Tₚ1", TRANSFER_PRIORITY.format(1));
  }
}
