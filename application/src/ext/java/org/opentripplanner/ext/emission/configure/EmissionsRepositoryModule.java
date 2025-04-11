package org.opentripplanner.ext.emission.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.emission.EmissionsRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionsRepository;

@Module
public class EmissionsRepositoryModule {

  @Provides
  @Singleton
  static EmissionsRepository provideEmissionsRepository() {
    return new DefaultEmissionsRepository();
  }
}
