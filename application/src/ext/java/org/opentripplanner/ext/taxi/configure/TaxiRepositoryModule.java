package org.opentripplanner.ext.taxi.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.taxi.TaxiRepository;
import org.opentripplanner.ext.taxi.internal.DefaultTaxiRepository;

@Module
public class TaxiRepositoryModule {

  @Provides
  @Singleton
  static TaxiRepository provideTaxiRepository() {
    return new DefaultTaxiRepository();
  }
}
