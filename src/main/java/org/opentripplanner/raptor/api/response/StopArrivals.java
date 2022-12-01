package org.opentripplanner.raptor.api.response;

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
   * The earliest arrival time at the given stop. If the stop is not reached, the behavior is
   * undefined; It may return an arbitrary value or throw an exception.
   * <p>
   * This is currently unused in OTP, but useful for analyses/comparing the results.
   */
  @SuppressWarnings("unused")
  int bestArrivalTime(int stopIndex);

  /**
   * Returns {@code true} if the stop is reached.
   */
  boolean reachedByTransit(int stopIndex);

  /**
   * The earliest transit arrival time at the given stop. If the stop is not reached by transit,
   * the behavior is undefined; It may return an arbitrary value or throw an exception.
   */
  int bestTransitArrivalTime(int stopIndex);

  /**
   * The smallest number of transfers needed to reach the given stop. If the stop is not reached,
   * the behavior is undefined; It may return an arbitrary value or throw an exception.
   * <p>
   * This is currently unused in OTP, but useful for analyses/comparing the results.
   */
  @SuppressWarnings("unused")
  int smallestNumberOfTransfers(int stopIndex);
}
