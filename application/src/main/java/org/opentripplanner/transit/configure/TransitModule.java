package org.opentripplanner.transit.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.standalone.api.HttpRequestScoped;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

@Module
public abstract class TransitModule {

  @Binds
  @HttpRequestScoped
  abstract TransitService bind(DefaultTransitService service);
}
