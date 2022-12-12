package org.opentripplanner.service.worldenvelope.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.worldenvelope.internal.WorldEnvelopeRepository;
import org.opentripplanner.service.worldenvelope.model.WorldEnvelopeService;
import org.opentripplanner.standalone.api.HttpRequestScoped;

@Module
public abstract class WorldEnvelopeModule {

  @Binds
  @HttpRequestScoped
  abstract WorldEnvelopeService bind(WorldEnvelopeRepository service);
}
