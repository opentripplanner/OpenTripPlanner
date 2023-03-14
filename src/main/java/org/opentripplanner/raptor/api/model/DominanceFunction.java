package org.opentripplanner.raptor.api.model;

/**
 * Represents a multi-criteria dominance function for comparing two int values:
 * {@code left} and {@code right}.
 */
@FunctionalInterface
public interface DominanceFunction {
  /**
   * Evaluates is left dominates right, if not return {@link false}.
   * <p>
   * Note! The function is not symmetric:
   * <ul>
   *   <li>If left dominate right, right may or may not dominate left.</li>
   *   <li>If left do not dominate right, right may or may not dominate left.</li>
   * </ul>
   */
  boolean leftDominateRight(int left, int right);
}
