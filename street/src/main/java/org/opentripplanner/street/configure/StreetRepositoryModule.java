package org.opentripplanner.street.configure;

import dagger.Binds;
import dagger.Module;
import org.opentripplanner.street.StreetRepository;
import org.opentripplanner.street.internal.DefaultStreetRepository;

@Module
public interface StreetRepositoryModule {
  @Binds
  StreetRepository bindStreetRepository(DefaultStreetRepository repository);
}
