package org.opentripplanner.raptor.rangeraptor.internalapi;

import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * The worker performs the travel search. There are multiple implementations, even some that do not
 * return paths.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RangeRaptorWorker<T extends RaptorTripSchedule> {
  /**
   * Fetch the result after the search is performed.
   */
  RaptorRouterResult<T> result();

  /**
   * Check if the RangeRaptor should continue with a new round.
   */
  boolean hasMoreRounds();

  /**
   * Perform a transit search for the current round.
   */
  void findTransitForRound();

  /**
   * Apply transfers for the current round.
   */
  void findTransfersForRound();

  /**
   * Return {@code true} if the destination is reached in the current round.
   */
  boolean isDestinationReachedInCurrentRound();

  /**
   * Apply access for the current round, including round zero - before the first transit.
   * This is applied in each round because the access may include transit (FLEX).
   */
  void findAccessOnStreetForRound();

  /**
   * Apply access for the current round, when the access arrives to the stop on-board (FLEX).
   */
  void findAccessOnBoardForRound();
}
