package org.opentripplanner.standalone.config.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.DeduplicatorService;

@Module
public class DeduplicatorServiceModule {

  @Provides
  @Singleton
  public static DeduplicatorService provideDeduplicatorService() {
    return new Deduplicator();
  }
}
