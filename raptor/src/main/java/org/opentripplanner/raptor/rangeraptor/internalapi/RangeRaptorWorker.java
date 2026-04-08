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
   * Check if the RangeRaptor should continue with a new round.
   */
  boolean hasMoreRounds();

  /**
   * Apply access for the current round, including round zero - before the first transit.
   * This is applied in each round because the access may include transit (FLEX).
   */
  void applyStreetStopAccess();

  /**
   * Apply access for the current round, when the access arrives to the stop on-board (FLEX).
   */
  void applyOnBoardStopAccess();

  /**
   * Find on-board access for round (accesses on-board an already started trip)
   */
  void applyOnBoardTripAccess(int iterationDepartureTime);

  /**
   * Perform a transit search for the current round.
   */
  void routeTransit();

  /**
   * Perform on-board (accesses on-board an already started trip) transit search for boardings and
   * alight events for the current round.
   */
  void routeTransitUsingOnBoardTripAccess();

  /**
   * Apply transfers for the current round.
   */
  void applyTransfers();

  /**
   * Return {@code true} if the destination is reached in the current round.
   */
  boolean isDestinationReachedInCurrentRound();

  /**
   * Fetch the result after the search is performed.
   */
  RaptorRouterResult<T> result();
}
