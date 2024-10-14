package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * These are the different cost configuration Raptor support. Each configuration will
 * be used to change the pareto-function used to compare arrivals and paths. We add
 * new values here when needed by a new use-case.
 */
public enum ParetoSetCost {
  /**
   * Cost is not used.
   */
  NONE,
  /**
   * One cost parameter is used. A small c1 value is better than a large value.
   */
  USE_C1,
  /**
   * Same as {@link #USE_C1}, but the relax function is used to relax the cost at the destination.
   * DO not use this! This will be removed as soon as the Vy, Entur, Norway has migrated off
   * this feature.
   */
  @Deprecated
  USE_C1_RELAX_DESTINATION,
  /**
   * Use both c1 and c2 in the pareto function. A small value is better than a large one.
   */
  USE_C1_AND_C2,
  /**
   * Use c1 in the pareto function, but relax c1 is c2 is optimal. This allows slightly worse
   * c1 values if a path is considered better based on the c2 value. Another way of looking at
   * this, is that all paths are grouped by the c2 value. When two paths are compared inside a group
   * the normal c1 comparison is used, and when comparing paths from different groups the relaxed
   * c1 comparison is used.
   */
  USE_C1_RELAXED_IF_C2_IS_OPTIMAL;

  public boolean includeC1() {
    return this != NONE;
  }

  /**
   * Use c2 as input to the pareto function. The c2 value is used as a criteria, or it is used
   * to modify the function ({@link #USE_C1_RELAXED_IF_C2_IS_OPTIMAL}).
   */
  public boolean includeC2() {
    return this == USE_C1_AND_C2 || this == USE_C1_RELAXED_IF_C2_IS_OPTIMAL;
  }
}
