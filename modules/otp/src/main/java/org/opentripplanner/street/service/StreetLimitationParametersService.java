package org.opentripplanner.street.service;

/**
 * A service for fetching limitation parameters of the street graph.
 */
public interface StreetLimitationParametersService {
  /**
   * Get the graph wide maximum car speed.
   *
   * @return Maximum car speed in meters per second.
   */
  float getMaxCarSpeed();
}
