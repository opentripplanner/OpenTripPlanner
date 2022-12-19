package org.opentripplanner.raptor.spi;

import javax.annotation.Nonnull;

/**
 * The purpose of the TripScheduleBoardAlight is to represent the board/alight for a given trip at a
 * specific stop. This is used as a result for the trip search, but may also be used in other
 * situation where a search is unnecessary like a guaranteed transfer.
 * <p>
 * An instance of this class is passed on to the algorithm to perform the boarding and contain the
 * necessary information to do so.
 * <p>
 * The instance can represent both the result of a forward search and the result of a reverse
 * search. For a reverse search (searching backward in time) the trip arrival times should be used.
 * This is one of the things that allows for the algorithm to be generic, used in both cases.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorTripScheduleBoardOrAlightEvent<T extends RaptorTripSchedule> {
  /** Used to indicate that no trip is found. */
  int NOT_FOUND = -1;

  /**
   * The trip timetable index for the trip  found.
   * <p>
   * If not found {@link #NOT_FOUND} is returned.
   */
  int getTripIndex();

  /**
   * This i a reference to the trip found.
   */
  T getTrip();

  /**
   * Return the stop-position-in-pattern for the current trip board search.
   */
  int getStopPositionInPattern();

  /**
   * Return the stop index for the boarding position.
   */
  default int getBoardStopIndex() {
    return getTrip().pattern().stopIndex(getStopPositionInPattern());
  }

  /**
   * Get the board/alight time for the trip found. For a forward search the boarding time should be
   * returned, and for the reverse search the alight time should be returned.
   */
  int getTime();

  /**
   * For a regular boarding, return the earliest-board-time passed in to the trip search.
   * For boardings using a constrained transfer the trip search must calculate the
   * earliest-board-time, because it depends on the constraints.
   * <p>
   * For a reverse search this method should return the latest-alight-time.
   */
  int getEarliestBoardTime();

  /**
   * Return the transfer constrains for the transfer before this boarding. If there are no transfer
   * constraints assisiated with the boarding the {@link RaptorTransferConstraint#isRegularTransfer()}
   * is {@code true}.
   */
  @Nonnull
  RaptorTransferConstraint getTransferConstraint();

  /**
   * This method return true if no result is found, but the algorithm may continue
   * to search using another way of boarding. The result is NOT empty if it is forbidden.
   */
  boolean empty();

  /**
   * Create an empty event with the given {@code earliestBoardTime}.
   * <p>
   * Sometimes we need to override the search result and force an empty result. This
   * factory method is used to avoid creating new instances, and instead the search
   * can be cleared (if implementing the fly-weight pattern).
   */
  static <S extends RaptorTripSchedule> RaptorTripScheduleBoardOrAlightEvent<S> empty(
    final int earliestBoardTime
  ) {
    return new EmptyBoardOrAlightEvent<>(earliestBoardTime);
  }
}
