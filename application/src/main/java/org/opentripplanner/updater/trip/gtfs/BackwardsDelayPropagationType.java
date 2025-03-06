package org.opentripplanner.updater.trip.gtfs;

/**
 * The backwards delay propagation type for a GTFS RT trip updater.
 */
public enum BackwardsDelayPropagationType {
  /**
   * Default value. Only propagates delays backwards when it is required to ensure that the times
   * are increasing, and it sets the NO_DATA flag on the stops so these automatically updated times
   * are not exposed through APIs.
   */
  REQUIRED_NO_DATA,

  /**
   * Only propagates delays backwards when it is required to ensure that the times are increasing.
   * The updated times are exposed through APIs.
   */
  REQUIRED,

  /**
   * Propagates delays backwards on stops with no estimates regardless if it's required or not.
   * The updated times are exposed through APIs.
   */
  ALWAYS,
}
