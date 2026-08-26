package org.opentripplanner.ext.taxizone.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.taxizone.TaxiZoneRepository;
import org.opentripplanner.ext.taxizone.internal.DefaultTaxiZoneRepository;

@Module
public class TaxiZoneRepositoryModule {

  @Provides
  @Singleton
  static TaxiZoneRepository provideTaxiZoneRepository() {
    return new DefaultTaxiZoneRepository();
  }
}
