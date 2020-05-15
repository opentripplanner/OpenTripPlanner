package org.opentripplanner.routing.api.response;

public enum RoutingErrorCode {
  /**
   * The origin and destination is not connected by transit, regardless of search time.
   */
  NO_TRANSIT_CONNECTION,
  /**
   * Location was found, but it was located outside the street network.
   */
  //TODO Currently not in use
  OUTSIDE_BOUNDS,

  /**
   * The time provided was outside the available service period.
   */
  //TODO Currently not in use
  OUTSIDE_SERVICE_PERIOD,

  /**
   * The location could not be matched to a valid id or coordinate.
   */
  LOCATION_NOT_FOUND,

  /**
   * The location was found, but no stops could be found within the search radius.
   */
  NO_STOPS_IN_RANGE
}
