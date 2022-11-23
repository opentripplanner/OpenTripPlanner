package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Map;
import org.opentripplanner.raptor.spi.RaptorSlackProvider;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.transit.model.basic.TransitMode;

/**
 * This class provides transferSlack, boardSlack and alightSlack for the Raptor algorithm.
 * <p>
 * Implementation notes: The board-slack and alight-slack is kept in an array, indexed by the mode
 * ordinal, and not in a {@link Map}, because it is faster. The board-slack and alight-slack lookup
 * is done for every strop-arrival computation, and should be as fast as possible.
 */
public final class SlackProvider implements RaptorSlackProvider {

  /**
   * Keep a list of board-slack values for each mode.
   */
  private final int[] boardSlack;

  /**
   * Keep a list of alight-slack values for each mode. Kept in an array for performance
   */
  private final int[] alightSlack;

  /**
   * A constant value is used for alight slack.
   */
  private final int transferSlack;

  public SlackProvider(
    int transferSlack,
    DurationForEnum<TransitMode> boardSlack,
    DurationForEnum<TransitMode> alightSlack
  ) {
    this.transferSlack = transferSlack;
    this.boardSlack = slackByMode(boardSlack);
    this.alightSlack = slackByMode(alightSlack);
  }

  @Override
  public int transferSlack() {
    return transferSlack;
  }

  @Override
  public int boardSlack(int slackIndex) {
    return boardSlack[slackIndex];
  }

  @Override
  public int alightSlack(int slackIndex) {
    return alightSlack[slackIndex];
  }

  /* private methods */

  private static int[] slackByMode(DurationForEnum<TransitMode> slack) {
    int[] result = new int[TransitMode.values().length];
    for (TransitMode mode : TransitMode.values()) {
      result[SlackProvider.slackIndex(mode)] = (int) slack.valueOf(mode).toSeconds();
    }
    return result;
  }

  public static int slackIndex(final TransitMode mode) {
    return mode.ordinal();
  }
}
