package org.opentripplanner.raptor.spi;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

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
public interface RaptorBoardOrAlightEvent<T extends RaptorTripSchedule> {
  /**
   * The trip timetable index for the trip  found.
   * <p>
   * If not found {@link RaptorConstants#NOT_FOUND} is returned.
   */
  int tripIndex();

  /**
   * This i a reference to the trip found.
   */
  T trip();

  /**
   * Return the stop-position-in-pattern for the current trip board search.
   */
  int stopPositionInPattern();

  /**
   * Return the stop index for the boarding position.
   */
  default int boardStopIndex() {
    return trip().pattern().stopIndex(stopPositionInPattern());
  }

  /**
   * Get the board/alight time for the trip found. For a forward search the boarding time should be
   * returned, and for the reverse search the alight time should be returned.
   */
  int time();

  /**
   * For a regular boarding, return the earliest-board-time passed in to the trip search.
   * For boardings using a constrained transfer the trip search must calculate the
   * earliest-board-time, because it depends on the constraints.
   * <p>
   * For a reverse search this method should return the latest-alight-time.
   */
  int earliestBoardTime();

  /**
   * Return the transfer constrains for the transfer before this boarding. If there are no transfer
   * constraints assisiated with the boarding the {@link RaptorTransferConstraint#isRegularTransfer()}
   * is {@code true}.
   */
  @Nonnull
  RaptorTransferConstraint transferConstraint();

  /**
   * This method return true if no result is found, but the algorithm may continue
   * to search using another way of boarding. The result is NOT empty if it is forbidden.
   */
  boolean empty();

  /**
   * This is a helper method for the Raptor implementation to be able to board or execute
   * a alternativeBoardingFallback method depending on the event. This logic should ideally
   * be put inside raptor, but due to performance(creating lambda instances, which for some
   * reason is not inlined) this need to be here.
   * <p>
   * @param boardCallback perform boarding if the event in none empty (or some other special
   *                      condition depending on the event, like boarding not allowed).
   * @param alternativeBoardingFallback This is executed if it is allowed to board according to
   *                                    this event and if the boarding event is empty.
   */
  default void boardWithFallback(
    Consumer<RaptorBoardOrAlightEvent<T>> boardCallback,
    Consumer<RaptorBoardOrAlightEvent<T>> alternativeBoardingFallback
  ) {
    if (empty()) {
      alternativeBoardingFallback.accept(this);
    } else {
      boardCallback.accept(this);
    }
  }

  /**
   * Create an empty event with the given {@code earliestBoardTime}.
   * <p>
   * Sometimes we need to override the search result and force an empty result. This
   * method can be used in none-performance critical parts of the code. The preferred
   * way for {@link RaptorConstrainedBoardingSearch}s is to clear its state and
   * implement the fly-weight pattern, avoiding creating new instances.
   */
  static <S extends RaptorTripSchedule> RaptorBoardOrAlightEvent<S> empty(
    final int earliestBoardTime
  ) {
    return new EmptyBoardOrAlightEvent<>(earliestBoardTime);
  }
}
