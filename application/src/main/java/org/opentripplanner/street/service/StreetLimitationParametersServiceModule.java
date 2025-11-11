package org.opentripplanner.street.service;

import dagger.Binds;
import dagger.Module;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public interface StreetLimitationParametersServiceModule {
  @Binds
  public StreetLimitationParametersService provideStreetLimitationParametersService(
    DefaultStreetLimitationParametersService it
  );
}
