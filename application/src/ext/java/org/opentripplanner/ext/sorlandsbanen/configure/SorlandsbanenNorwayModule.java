package org.opentripplanner.ext.sorlandsbanen.configure;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import org.opentripplanner.ext.sorlandsbanen.SorlandsbanenNorwayService;
import org.opentripplanner.framework.application.OTPFeature;

@Module
public class SorlandsbanenNorwayModule {

  @Provides
  @Nullable
  SorlandsbanenNorwayService providesSorlandsbanenNorwayService() {
    return OTPFeature.Sorlandsbanen.isOn() ? new SorlandsbanenNorwayService() : null;
  }
}
