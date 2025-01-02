package org.opentripplanner.ext.stopconsolidation.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationRepository;
import org.opentripplanner.ext.stopconsolidation.internal.DefaultStopConsolidationRepository;

/**
 * The repository is used during application loading phase, so we need to provide
 * a module for the repository.
 */
@Module
public interface StopConsolidationRepositoryModule {
  @Binds
  StopConsolidationRepository bindRepository(DefaultStopConsolidationRepository repo);
}
