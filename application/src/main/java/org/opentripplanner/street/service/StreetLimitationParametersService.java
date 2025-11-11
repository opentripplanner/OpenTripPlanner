package org.opentripplanner.street.service;

import org.opentripplanner.street.internal.DefaultStreetRepository;

/**
 * A service for fetching limitation parameters of the street graph.
 */
public interface StreetLimitationParametersService {
  StreetLimitationParametersService DEFAULT = new DefaultStreetLimitationParametersService(
    new DefaultStreetRepository()
  );

  /**
   * Get the graph wide maximum car speed.
   *
   * @return Maximum car speed in meters per second.
   */
  float maxCarSpeed();

  int maxAreaNodes();

  /**
   * Get the graph wide best walk safety.
   */
  float getBestWalkSafety();

  /**
   * Get the graph wide best bike safety.
   */
  float getBestBikeSafety();
}
