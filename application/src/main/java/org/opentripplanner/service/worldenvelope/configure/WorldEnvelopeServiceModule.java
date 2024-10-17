package org.opentripplanner.service.worldenvelope.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.worldenvelope.WorldEnvelopeService;
import org.opentripplanner.service.worldenvelope.internal.DefaultWorldEnvelopeService;

/**
 * The service is used during application serve phase, not loading, so we need to provide
 * a module for the service without the repository, which is injected from the loading phase.
 */
@Module
public interface WorldEnvelopeServiceModule {
  @Binds
  WorldEnvelopeService bindService(DefaultWorldEnvelopeService service);
}
