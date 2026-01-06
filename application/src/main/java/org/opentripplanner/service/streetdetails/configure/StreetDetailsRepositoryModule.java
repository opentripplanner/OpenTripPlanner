package org.opentripplanner.service.streetdetails.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.service.streetdetails.StreetDetailsRepository;
import org.opentripplanner.service.streetdetails.internal.DefaultStreetDetailsRepository;

@Module
public interface StreetDetailsRepositoryModule {
  @Binds
  StreetDetailsRepository bind(DefaultStreetDetailsRepository repository);
}
