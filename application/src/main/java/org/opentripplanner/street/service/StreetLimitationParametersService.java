package org.opentripplanner.street.service;

import static org.opentripplanner.street.model.StreetConstants.DEFAULT_MAX_CAR_SPEED;

/**
 * A service for fetching limitation parameters of the street graph.
 */
public interface StreetLimitationParametersService {
  StreetLimitationParametersService DEFAULT = new StreetLimitationParametersService() {
    @Override
    public float getMaxCarSpeed() {
      return DEFAULT_MAX_CAR_SPEED;
    }
    
    @Override
    public int maxAreaNodes() {
      return 150;
    }

    @Override
    public float getBestWalkSafety() {
      return 1.0f;
    }

    @Override
    public float getBestBikeSafety() {
      return 1.0f;
    }
  };

  /**
   * Get the graph wide maximum car speed.
   *
   * @return Maximum car speed in meters per second.
   */
  float getMaxCarSpeed();

  public int maxAreaNodes();

  /**
   * Get the graph wide best walk safety.
   */
  float getBestWalkSafety();

  /**
   * Get the graph wide best bike safety.
   */
  float getBestBikeSafety();
}
