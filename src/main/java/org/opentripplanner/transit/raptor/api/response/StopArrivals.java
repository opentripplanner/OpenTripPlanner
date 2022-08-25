package org.opentripplanner.transit.raptor.api.response;

/**
 * Provide basic information for all stops reach in a Raptor search. This can be used to visualize
 * the search.
 */
public interface StopArrivals {
  /**
   * Returns {@code true} if the stop is reached.
   */
  boolean reached(int stopIndex);

  /**
   * Returns {@code true} if the stop is reached.
   */
  boolean reachedByTransit(int stopIndex);

  /**
   * The earliest transit arrival time at the given stop. If the stop is not reached by transit,
   * the behavior is undefined; It may return an arbitrary value or throw an exception.
   */
  int bestTransitArrivalTime(int stopIndex);
}
