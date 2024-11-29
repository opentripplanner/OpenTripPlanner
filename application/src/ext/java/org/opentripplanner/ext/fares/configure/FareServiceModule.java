package org.opentripplanner.ext.fares.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.standalone.config.BuildConfig;

@Module
public class FareServiceModule {
  @Provides
  FareService bindRepository(BuildConfig config) {
    return config.fareServiceFactory.makeFareService();
  };
}
