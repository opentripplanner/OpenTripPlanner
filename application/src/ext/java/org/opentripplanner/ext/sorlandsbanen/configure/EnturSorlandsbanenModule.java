package org.opentripplanner.ext.sorlandsbanen.configure;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nullable;
import org.opentripplanner.ext.sorlandsbanen.EnturSorlandsbanenService;
import org.opentripplanner.framework.application.OTPFeature;

@Module
public class EnturSorlandsbanenModule {

  @Provides
  @Nullable
  EnturSorlandsbanenService providesEnturSorlandsbanenService() {
    return OTPFeature.Sorlandsbanen.isOn() ? new EnturSorlandsbanenService() : null;
  }
}
