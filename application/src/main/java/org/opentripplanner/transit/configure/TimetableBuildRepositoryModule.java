package org.opentripplanner.transit.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.transit.repository.TimetableBuildRepository;

/**
 * Provides the empty {@link TimetableBuildRepository} populated by the graph build. When a graph
 * is loaded, the deserialized instance is used instead.
 */
@Module
public class TimetableBuildRepositoryModule {

  @Provides
  @Singleton
  public TimetableBuildRepository provideTimetableBuildRepository() {
    return new TimetableBuildRepository();
  }
}
