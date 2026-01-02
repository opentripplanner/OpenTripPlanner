package org.opentripplanner.service.streetdetails.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsService;

@Module
public interface StreetDetailsServiceModule {
  @Binds
  StreetDetailsService bind(DefaultStreetDetailsService service);
}
