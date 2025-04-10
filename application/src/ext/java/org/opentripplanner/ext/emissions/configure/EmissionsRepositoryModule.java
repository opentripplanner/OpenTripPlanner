package org.opentripplanner.ext.emissions.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.emissions.EmissionsRepository;
import org.opentripplanner.ext.emissions.internal.DefaultEmissionsRepository;

@Module
public class EmissionsRepositoryModule {

  @Provides
  @Singleton
  static EmissionsRepository provideEmissionsRepository() {
    return new DefaultEmissionsRepository();
  }
}
