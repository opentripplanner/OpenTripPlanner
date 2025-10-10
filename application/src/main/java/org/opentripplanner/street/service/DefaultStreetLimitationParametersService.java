package org.opentripplanner.street.service;

import jakarta.inject.Singleton;
import org.opentripplanner.street.model.StreetLimitationParameters;

@Singleton
public class DefaultStreetLimitationParametersService implements StreetLimitationParametersService {

  private final StreetLimitationParameters streetLimitationParameters;

  public DefaultStreetLimitationParametersService(
    StreetLimitationParameters streetLimitationParameters
  ) {
    this.streetLimitationParameters = streetLimitationParameters;
  }

  @Override
  public float getMaxCarSpeed() {
    return streetLimitationParameters.maxCarSpeed();
  }

  @Override
  public float getBestWalkSafety() {
    return streetLimitationParameters.bestWalkSafety();
  }

  @Override
  public float getBestBikeSafety() {
    return streetLimitationParameters.bestBikeSafety();
  }
}
