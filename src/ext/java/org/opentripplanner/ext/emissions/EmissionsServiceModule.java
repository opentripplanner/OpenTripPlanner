package org.opentripplanner.ext.emissions;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public class EmissionsServiceModule {

  @Provides
  @Singleton
  public EmissionsService provideEmissionsService(EmissionsDataModel emissionsDataModel) {
    return new DefaultEmissionsService(emissionsDataModel);
  }
}
