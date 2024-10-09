package org.opentripplanner.raptor.spi;

/**
 * A simple default implementation which can be used when board and alight slack is fixed.
 * All field unit are in seconds.
 */
public class DefaultSlackProvider implements RaptorSlackProvider {

  private final int transferSlack;
  private final int boardSlack;
  private final int alightSlack;

  public DefaultSlackProvider(int transferSlack, int boardSlack, int alightSlack) {
    this.transferSlack = transferSlack;
    this.boardSlack = boardSlack;
    this.alightSlack = alightSlack;
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
