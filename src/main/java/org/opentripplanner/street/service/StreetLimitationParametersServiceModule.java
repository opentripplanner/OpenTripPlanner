package org.opentripplanner.street.service;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.street.model.StreetLimitationParameters;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public class StreetLimitationParametersServiceModule {

  @Provides
  @Singleton
  public StreetLimitationParametersService provideStreetLimitationParametersService(
    StreetLimitationParameters streetLimitationParameters
  ) {
    return new DefaultStreetLimitationParametersService(streetLimitationParameters) {};
  }
}
