package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import org.opentripplanner.raptor.spi.RaptorSlackProvider;

/**
 * This is a simpler version than the {@link DefaultSlackProvider}. Raptor is not under test, so
 * there is no need for the complex version in most unit-tests.
 *
 * FOR TESTS ONLY
 */
public class TestSlackProvider implements RaptorSlackProvider {

  private final int boardSlack;
  private final int alightSlack;
  private final int transferSlack;

  public TestSlackProvider(int boardSlack, int alightSlack, int transferSlack) {
    this.boardSlack = boardSlack;
    this.alightSlack = alightSlack;
    this.transferSlack = transferSlack;
  }

  @Override
  public int transferSlack() {
    return transferSlack;
  }

  @Override
  public int boardSlack(int slackIndex) {
    return boardSlack;
  }

  @Override
  public int alightSlack(int slackIndex) {
    return alightSlack;
  }
}
