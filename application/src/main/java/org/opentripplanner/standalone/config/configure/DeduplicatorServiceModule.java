package org.opentripplanner.standalone.config.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.model.framework.Deduplicator;

@Module
public class DeduplicatorServiceModule {

  @Provides
  @Singleton
  public static DeduplicatorService provideDeduplicatorService() {
    return new Deduplicator();
  }
}
