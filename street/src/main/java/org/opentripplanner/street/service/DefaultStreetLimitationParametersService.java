package org.opentripplanner.street.service;

import jakarta.inject.Inject;
import org.opentripplanner.street.StreetRepository;

public class DefaultStreetLimitationParametersService implements StreetLimitationParametersService {

  private final StreetRepository streetRepository;

  @Inject
  public DefaultStreetLimitationParametersService(StreetRepository streetRepository) {
    this.streetRepository = streetRepository;
  }

  @Override
  public float maxCarSpeed() {
    return streetRepository.streetModelDetails().maxCarSpeed();
  }

  @Override
  public int maxAreaNodes() {
    return streetRepository.streetModelDetails().maxAreaNodes();
  }
}
