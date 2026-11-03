package org.opentripplanner.ext.empiricaldelay.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.empiricaldelay.EmpiricalDelayRepository;
import org.opentripplanner.ext.empiricaldelay.internal.DefaultEmpiricalDelayRepository;

@Module
public class EmpiricalDelayRepositoryModule {

  @Provides
  @Singleton
  @Nullable
  static EmpiricalDelayRepository provideEmpiricalDelayRepository() {
    return new DefaultEmpiricalDelayRepository();
  }
}
