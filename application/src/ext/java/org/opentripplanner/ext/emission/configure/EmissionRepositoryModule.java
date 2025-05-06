package org.opentripplanner.ext.emission.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;

@Module
public class EmissionRepositoryModule {

  @Provides
  @Singleton
  static EmissionRepository provideEmissionRepository() {
    return new DefaultEmissionRepository();
  }
}
