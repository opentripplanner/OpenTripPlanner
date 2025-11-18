package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.model.framework.Deduplicator;

@Module
public class DeduplicatorServiceModule {

  @Provides
  public static DeduplicatorService provideDeduplicatorService() {
    return new Deduplicator();
  }
}
