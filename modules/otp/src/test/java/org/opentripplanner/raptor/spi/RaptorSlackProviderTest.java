package org.opentripplanner.raptor.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RaptorSlackProviderTest {

  private static final int[] BOARD_SLACK = new int[] { 0, 20 };
  private static final int[] ALIGHT_SLACK = new int[] { 0, 30 };

  private final RaptorSlackProvider subject = new RaptorSlackProvider() {
    @Override
    public int transferSlack() {
      return 10;
    }

    @Override
    public int boardSlack(int slackIndex) {
      return BOARD_SLACK[slackIndex];
    }

    @Override
    public int alightSlack(int slackIndex) {
      return ALIGHT_SLACK[slackIndex];
    }
  };

  @Test
  void calcRegularTransferDuration() {
    assertEquals(110, subject.calcRegularTransferDuration(100, 0, 0));
    assertEquals(130, subject.calcRegularTransferDuration(100, 0, 1));
    assertEquals(140, subject.calcRegularTransferDuration(100, 1, 0));
    assertEquals(160, subject.calcRegularTransferDuration(100, 1, 1));
  }
}
