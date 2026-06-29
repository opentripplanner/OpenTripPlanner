package org.opentripplanner.ext.carpickupzone.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.carpickupzone.CarPickupZoneRepository;
import org.opentripplanner.ext.carpickupzone.internal.DefaultCarPickupZoneRepository;

@Module
public class CarPickupZoneRepositoryModule {

  @Provides
  @Singleton
  static CarPickupZoneRepository provideCarPickupZoneRepository() {
    return new DefaultCarPickupZoneRepository();
  }
}
