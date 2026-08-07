package org.opentripplanner.raptor.spi;

import org.opentripplanner.utils.lang.IntUtils;

/**
 * This interface represents a trip pattern. A trip-pattern in the raptor context is just a list of
 * stops visited by ALL trips in the pattern. The stops must be ordered in the same sequence, with
 * no gaps, as the trips visit the stops.
 */
public interface RaptorTripPattern {
  int patternIndex();

  /**
   * Number of stops in the pattern.
   */
  int numberOfStopsInPattern();

  /**
   * The stop index
   *
   * @param stopPositionInPattern stop position number in the pattern, starting at 0.
   */
  int stopIndex(int stopPositionInPattern);

  /**
   * Return {@code true} if it is allowed/possible to board at a particular stop index. This should
   * include checks like: Does the pattern allow boarding at the given stop? Is this accessible to
   * wheelchairs (if requested).
   *
   * @param stopPositionInPattern stop position number in the pattern, starting at 0.
   */
  boolean boardingPossibleAt(int stopPositionInPattern);

  /**
   * Same as {@link #boardingPossibleAt(int)}, but for getting off a trip.
   *
   * @param stopPositionInPattern stop position number in the pattern, starting at 0.
   */
  boolean alightingPossibleAt(int stopPositionInPattern);

  /**
   * Alight and board slack is added by raptor based on this index. The index allows the data
   * provider to setup and use different board and alight slacks for each "type" of
   * RaptorTripPattern. The {@link RaptorSlackProvider} is used to return the actual slack.
   */
  int slackIndex();

  /**
   * A pattern may belong to a transit-group-priority. Each group is given an advantage during
   * the multi-criteria search, so the best alternative for each group is found.
   */
  int priorityGroupId();

  /**
   * Pattern debug info, return transit mode and route name. This is used for debugging purposes
   * only. The implementation should provide a short description with enough information for humans
   * to identify the trip/route. This is used in a context where information about agency and
   * stop is known, so there is no need to include agency or geographical region information.
   * <p/>
   * The recommended string to return is: {@code [MODE] [SHORT_ROUTE_DESCRIPTION]}.
   */
  String debugInfo();

  /**
   * Return the first occurrence of the stop position for the given stop index after the given
   * startPosition(exclusive) where alighting is possible.
   * <p>
   * {@code -1} is returned if not found.
   *
   * @param startPos  the stop position in the pattern to start the search (exclusive). Note! Only
   *                  defined for range {@code [0..N]}  (N = number of stops in the pattern).
   * @param alightStopIndex the stopIndex to find
   */
  default int findAlightStopPositionBefore(int startPos, int alightStopIndex) {
    IntUtils.requireInRange(startPos, 0, numberOfStopsInPattern(), "startPos");
    for (int i = startPos - 1; i > 0; --i) {
      if (alightStopIndex == stopIndex(i) && alightingPossibleAt(i)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the first occurrence of the stop position for the given stop index after the given
   * startPos(exclusive) where alighting is possible. Note that the returned value might not be the
   * only occurrence if the pattern goes in a loop.
   * <p>
   * {@code -1} is returned if not found.
   *
   * @param startPos  the stop position in the pattern to start the search (exclusive). Note! Only
   *                  defined for range {@code [0..N-1]} (N = number of stops in the pattern).
   * @param alightStopIndex the stopIndex to find
   */
  default int findAlightStopPositionAfter(int startPos, int alightStopIndex) {
    IntUtils.requireInRange(startPos, 0, numberOfStopsInPattern() - 1, "startPos");
    for (int i = startPos + 1; i < numberOfStopsInPattern(); ++i) {
      if (alightStopIndex == stopIndex(i) && alightingPossibleAt(i)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the first occurrence of the stop position for the given stop index before the given
   * startPos(inclusive). Note that the returned value might not be the only occurrence if the
   * pattern goes in a loop.
   * <p>
   * {@code -1} is returned if not found.
   *
   * @param startPos  the stop position in the pattern to start the search (inclusive)
   * @param stopIndex the stopIndex to find
   */
  default int findStopPositionBefore(int startPos, int stopIndex) {
    for (int i = startPos; i >= 0; i--) {
      if (stopIndex == stopIndex(i)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the first occurrence of the stop position for the given stop index before the given
   * startPos(exclusive). Note that the returned value might not be the only occurrence if the
   * pattern goes in a loop.
   * <p>
   * {@code -1} is returned if not found.
   *
   * @param startPos  the stop position in the pattern to start the search (exclusive). Note! Only
   *                  defined for range {@code [0..N-1]} (N = number of stops in the pattern).
   * @param boardStopIndex the stopIndex to find
   */
  default int findBoardStopPositionBefore(int startPos, int boardStopIndex) {
    IntUtils.requireInRange(startPos, 0, numberOfStopsInPattern() - 1, "startPos");
    for (int i = startPos - 1; i >= 0; --i) {
      if (boardStopIndex == stopIndex(i) && boardingPossibleAt(i)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the first occurrence of the stop position for the given stop index at or after the
   * given startPos(inclusive). Note that the returned value might not be the only occurrence if
   * the pattern goes in a loop.
   * <p>
   * {@code -1} is returned if not found.
   *
   * @param startPos the stop position in the pattern to start the search (inclusive). Note! Only
   *    *            defined for range {@code [0..N-1]} (N = number of stops in the pattern).
   * @param boardStopIndex the stopIndex to find
   */
  default int findBoardStopPositionAfter(int startPos, int boardStopIndex) {
    IntUtils.requireInRange(startPos, 0, numberOfStopsInPattern() - 1, "startPos");
    for (int i = startPos; i < numberOfStopsInPattern() - 1; ++i) {
      if (boardStopIndex == stopIndex(i) && boardingPossibleAt(i)) {
        return i;
      }
    }
    return -1;
  }
}
