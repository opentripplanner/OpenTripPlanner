package org.opentripplanner.ext.emission.configure;

import static org.opentripplanner.ext.emission.model.CarEmissionUtil.calculateCarCo2EmissionPerMeterPerPerson;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.internal.DefaultEmissionRepository;
import org.opentripplanner.standalone.config.BuildConfig;

@Module
public class EmissionRepositoryModule {

  @Provides
  @Singleton
  static EmissionRepository provideEmissionRepository(BuildConfig config) {
    var repository = new DefaultEmissionRepository();
    // Init car passenger emission data
    {
      repository.setCarAvgCo2PerMeter(
        calculateCarCo2EmissionPerMeterPerPerson(config.emission.car())
      );
    }
    return repository;
  }
}
