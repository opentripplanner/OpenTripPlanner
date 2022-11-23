package org.opentripplanner.raptor.spi;

import javax.annotation.Nullable;

/**
 * The purpose of the TripScheduleSearch is to search for a trip schedule for a given pattern.
 * The search need to be optimized for speed, this is one of the most frequently called
 * operations in Raptor and accessing objects in memory should be avoided.
 * <p/>
 * There should be two implementations of this interface, one for board-times and one for
 * alight-times. When Raptor search in reverse direction the alight-time search should be used. For
 * a reverse search (searching backward in time) the trip found departure/arrival times are swapped.
 * This is one of the things that allows for the Raptor implementation to be generic, used in both
 * cases.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorTripScheduleSearch<T extends RaptorTripSchedule> {
  /** Used in a trip search to indicate that all trips should be included in the search. */
  int UNBOUNDED_TRIP_INDEX = -1;

  /**
   * Find the best trip matching the given {@code timeLimit}. This is the same as calling {@link
   * #search(int, int, int)} with {@code tripIndexLimit: -1}.
   *
   * @see #search(int, int, int)
   */
  @Nullable
  @Flyweight
  default RaptorTripScheduleBoardOrAlightEvent<T> search(
    int earliestBoardTime,
    int stopPositionInPattern
  ) {
    return search(earliestBoardTime, stopPositionInPattern, UNBOUNDED_TRIP_INDEX);
  }

  /**
   * Find the best trip matching the given {@code timeLimit} and {@code tripIndexLimit}. This method
   * returns {@code null} if no trip is found.
   * <p>
   * Note! The implementation may use a "fly-weight" pattern to implement this, which mean no
   * objects are created for the result, but the result object will instead be reused for the next
   * search. So, the caller MUST copy values over and NOT store references to the result object. As
   * soon as a new call the search is done, the result object is invalid.
   *
   * @param earliestBoardTime     The time of arrival(departure for reverse search) at the given
   *                              stop.
   * @param stopPositionInPattern The stop to board
   * @param tripIndexLimit        Upper bound for trip index to search. Inclusive. Use {@code -1}
   *                              for an unbounded search. This is an optimization which allow us to
   *                              search faster, and it exclude results which is less favorable than
   *                              trips already processed.
   */
  @Nullable
  @Flyweight
  RaptorTripScheduleBoardOrAlightEvent<T> search(
    int earliestBoardTime,
    int stopPositionInPattern,
    int tripIndexLimit
  );
}
