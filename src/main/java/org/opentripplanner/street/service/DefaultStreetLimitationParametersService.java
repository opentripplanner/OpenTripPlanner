package org.opentripplanner.street.service;

import jakarta.inject.Inject;
import org.opentripplanner.street.model.StreetLimitationParameters;

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
