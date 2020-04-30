package org.opentripplanner.routing.api.response;

public enum RoutingErrorCode {
  /**
   * Location was found, but it was located outside the street network.
   */
  OUTSIDE_BOUNDS,

  /**
   * The time provided was outside the available service period.
   */
  OUTSIDE_SERVICE_PERIOD,

  /**
   * The location could not be matched to a valid id or coordinate.
   */
  LOCATION_NOT_FOUND,

  /**
   * The location was found, but no stops could be connected within the search radius.
   */
  NO_STOPS_IN_RANGE
}
