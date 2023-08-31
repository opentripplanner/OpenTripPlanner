package org.opentripplanner.ext.digitransitemissions;

import dagger.Binds;
import dagger.Module;

@Module
public interface EmissionsServiceRepositoryModule {
  @Binds
  EmissionsServiceRepository bindRepository(DefaultEmissionsServiceRepository repository);
}
