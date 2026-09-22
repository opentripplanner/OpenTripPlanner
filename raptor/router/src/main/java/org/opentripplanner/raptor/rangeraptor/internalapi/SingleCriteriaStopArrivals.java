package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * This interface is used to access the state produced by a Raptor for a given single criterion
 * for the best-overall-arrivals or best-transit-arrivals. The unit can be time, duration, any
 * generalized-cost and/or number-of-transfers.
 */
public interface SingleCriteriaStopArrivals {
  /**
   * Return true if the stop is reached (by access, transfer or transit).
   */
  boolean isReached(int stop);

  /**
   * The "overall" best value for the criteria at the given stop.
   */
  int value(int stop);
}
