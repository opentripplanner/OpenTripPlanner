package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

/**
 * This interface allow us to implement different strategies for calculating the transit cost, or
 * concrete provide a transit-cost-reluctance by index.
 * <p>
 * The class and methods are {@code final} to help the JIT compiler optimize the use of this class.
 */
interface FactorStrategy {
  /**
   * Return the factor for the given index.
   */
  int factor(int index);

  /**
   * The minimum factor is used to calculate heuristics. It need to be the smallest factor in use
   * (across all indexes).
   */
  int minFactor();
}
