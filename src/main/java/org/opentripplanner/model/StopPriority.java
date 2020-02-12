package org.opentripplanner.model;

/**
 * This is equivalent to NeTEx InterchangeWeightingEnumeration which can apply to both StopPlace
 * and Interchange. This enum is currently only meant to be used for Stop priority. The routing
 * algorithm should then assign different costs to boarding and alighting at stops according to
 * these priority levels.
 *
 * GTFS currently does not support stop priority.
 */
public enum StopPriority {
  /**
   * The stop should have the highest possible priority.
   */
  PREFERRED,
  /**
   * The stop should be prioritized over other stops.
   */
  RECOMMENDED,
  /**
   * The standard level of stop priority.
   */
  ALLOWED,
  /**
   * Use if this stop is discouraged.
   */
  DISCOURAGED
}
