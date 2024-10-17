package org.opentripplanner.transit.model.site;

/**
 * The type of a stop location.
 */
public enum StopType {
  /**
   * A regular stop defined geographically as a point.
   */
  REGULAR,
  /**
   * Boarding and alighting is allowed anywhere within the geographic area of this stop.
   */
  FLEXIBLE_AREA,
  /**
   * A stop that consists of multiple other stops, area or regular.
   */
  FLEXIBLE_GROUP,
}
