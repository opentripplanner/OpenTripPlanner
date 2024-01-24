package org.opentripplanner.transit.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.standalone.api.OtpServerRequestScope;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;

@Module
public abstract class TransitModule {

  @Binds
  @OtpServerRequestScope
  abstract TransitService bind(DefaultTransitService service);
}
