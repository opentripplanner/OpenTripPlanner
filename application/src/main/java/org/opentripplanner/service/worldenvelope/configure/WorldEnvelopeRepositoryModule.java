package org.opentripplanner.service.worldenvelope.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeRepository;

/**
 * The repository is used during application loading phase, so we need to provide
 * a module for the repository as well as the service.
 */
@Module
public interface WorldEnvelopeRepositoryModule {
  @Binds
  WorldEnvelopeRepository bindRepository(DefaultWorldEnvelopeRepository repository);
}
