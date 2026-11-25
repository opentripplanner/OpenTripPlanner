package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTransfer;
import org.opentripplanner.raptor._data.transit.TestTransitData;

class RaptorTransferDataProviderFindMaterializedTransferTest implements RaptorTestConstants {

  @Test
  void noTransferToStop() {
    var data = new TestTransitData().withTransfer(STOP_A, TestTransfer.transfer(STOP_B, 60, 100));
    assertNull(data.findMaterializedTransfer(STOP_A, STOP_C, 600));
    assertNull(data.findMaterializedTransfer(STOP_B, STOP_A, 600));
  }

  @Test
  void durationLimitIsInclusive() {
    var data = new TestTransitData().withTransfer(STOP_A, TestTransfer.transfer(STOP_B, 60, 100));
    assertNull(data.findMaterializedTransfer(STOP_A, STOP_B, 59));
    assertEquals(60, data.findMaterializedTransfer(STOP_A, STOP_B, 60).durationInSeconds());
  }

  @Test
  void lowestC1Wins() {
    var data = new TestTransitData()
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, 30, 500))
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, 60, 200))
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_C, 10, 1));

    var tx = data.findMaterializedTransfer(STOP_A, STOP_B, 600);
    assertEquals(STOP_B, tx.stop());
    assertEquals(200, tx.c1());
    assertEquals(60, tx.durationInSeconds());
  }

  @Test
  void shortestDurationBreaksC1Tie() {
    var data = new TestTransitData()
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, 60, 200))
      .withTransfer(STOP_A, TestTransfer.transfer(STOP_B, 30, 200));

    assertEquals(30, data.findMaterializedTransfer(STOP_A, STOP_B, 600).durationInSeconds());
  }

  @Test
  void resultIsMaterialized() {
    var data = new TestTransitData().withTransfer(STOP_A, TestTransfer.transfer(STOP_B, 60, 100));
    var tx = data.findMaterializedTransfer(STOP_A, STOP_B, 600);
    assertNotNull(tx);
    assertEquals(TestTransfer.transfer(STOP_B, 60, 100), tx);
  }
}
