package org.opentripplanner.street.model;

import jakarta.inject.Inject;

public class DefaultStreetLimitationParametersService implements StreetLimitationParametersService {

  private final StreetLimitationParameters streetLimitationParameters;

  @Inject
  public DefaultStreetLimitationParametersService(
    StreetLimitationParameters streetLimitationParameters
  ) {
    this.streetLimitationParameters = streetLimitationParameters;
  }

  @Override
  public float getMaxCarSpeed() {
    return streetLimitationParameters.maxCarSpeed();
  }
}
