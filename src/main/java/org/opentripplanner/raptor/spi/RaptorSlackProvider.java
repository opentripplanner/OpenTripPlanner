package org.opentripplanner.raptor.spi;

/**
 * Responsible for providing {@code boardSlack}, {@code alightSlack} and {@code transferSlack}.
 */
public interface RaptorSlackProvider {
  /**
   * Return a default implementation which can be used in unit-tests. Unit: seconds.
   */
  static RaptorSlackProvider defaultSlackProvider(
    int transferSlack,
    int boardSlack,
    int alightSlack
  ) {
    return new RaptorSlackProvider() {
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
    };
  }

  /**
   * The transfer-slack (duration time in seconds) to add between transfers. This is in addition to
   * {@link #boardSlack(int)} and {@link #alightSlack(int)}.
   * <p>
   * Unit: seconds.
   */
  int transferSlack();

  /**
   * The board-slack (duration time in seconds) to add to the stop arrival time, before boarding the
   * given trip pattern.
   * <p>
   * Implementation notes: In a forward-search the pattern is known, but not the trip (You must
   * calculate the earliest-bord-time before boarding).
   * <p>
   * Unit: seconds.
   */
  int boardSlack(int slackIndex);

  /**
   * The alight-slack (duration time in seconds) to add to the trip alight time for the given
   * pattern when calculating the the stop-arrival-time.
   * <p>
   * Implementation notes: In a reverse-search the pattern is known, but not the trip (You must
   * calculate the latest-alight-time before finding the trip-by-arriving-time).
   * <p>
   * Unit: seconds.
   */
  int alightSlack(int slackIndex);

  /**
   * Return the {@link #boardSlack(int) plus {@link #alightSlack(int)
   * slack.
   * <p>
   * Unit: seconds.
   */
  default int transitSlack(int slackIndex) {
    return boardSlack(slackIndex) + alightSlack(slackIndex);
  }

  /**
   * Calculate regular transfer duration including slack.
   */
  default int calcRegularTransferDuration(
    int transferDurationInSeconds,
    int fromTripAlightSlackIndex,
    int toTripBoardSlackIndex
  ) {
    return (
      alightSlack(fromTripAlightSlackIndex) +
      transferDurationInSeconds +
      transferSlack() +
      boardSlack(toTripBoardSlackIndex)
    );
  }
}
