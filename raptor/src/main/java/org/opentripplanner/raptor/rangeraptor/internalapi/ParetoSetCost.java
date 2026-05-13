package org.opentripplanner.raptor.rangeraptor.internalapi;

/**
 * These are the different cost configuration Raptor support. Each configuration will
 * be used to change the pareto-function used to compare arrivals and paths. We add
 * new values here when needed by a new use-case.
 */
public enum ParetoSetCost {
  /**
   * Cost is not used. This can not be used with McRaptor.
   */
  NONE,
  /**
   * One cost parameter is used. A small c1 value is better than a large value.
   */
  USE_C1,
  /**
   * Use c1 in the pareto function, but relax c1 is c2 is optimal. This allows slightly worse
   * c1 values if a path is considered better based on the c2 value. Another way of looking at
   * this, is that all paths are grouped by the c2 value. When two paths are compared inside a group
   * the normal c1 comparison is used, and when comparing paths from different groups the relaxed
   * c1 comparison is used.
   */
  USE_C1_RELAXED_IF_C2_IS_OPTIMAL;

  /// Return `true` if the c1 criteria is included in the pareto comparason.
  public boolean useC1() {
    return this != NONE;
  }
}
