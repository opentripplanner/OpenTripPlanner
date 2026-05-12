package org.opentripplanner.ext.sorlandsbanen.configure;

import dagger.Module;
import dagger.Provides;
import java.util.Optional;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.framework.application.OTPFeature;

@Module
public class SorlandsbanenNorwayModule {

  @Provides
  Optional<SorlandsbanenNorwayService> providesSorlandsbanenNorwayService() {
    return OTPFeature.Sorlandsbanen.isOn()
      ? Optional.of(new SorlandsbanenNorwayService())
      : Optional.empty();
  }
}
