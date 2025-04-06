package org.opentripplanner.ext.fares.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.routing.fares.FareServiceFactory;
import org.opentripplanner.standalone.config.BuildConfig;

@Module
public class FareModule {

  @Provides
  @Singleton
  public FareServiceFactory factory(BuildConfig config) {
    return config.fareServiceFactory;
  }
}
