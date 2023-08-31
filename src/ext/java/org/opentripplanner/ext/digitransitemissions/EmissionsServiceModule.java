package org.opentripplanner.ext.digitransitemissions;

import dagger.Binds;
import dagger.Module;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public interface EmissionsServiceModule {
  @Binds
  EmissionsService bindService(DefaultEmissionsService service);
}
