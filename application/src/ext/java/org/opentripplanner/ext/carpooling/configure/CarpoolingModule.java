package org.opentripplanner.ext.carpooling.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.framework.application.OTPFeature;

/**
 * TODO CARPOOLING
 */
@Module
public class CarpoolingModule {

  @Provides
  @Singleton
  public CarpoolingRepository provideCarpoolingRepository() {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    // TODO CARPOOLING
    return null;
  }

  @Provides
  public static CarpoolingService provideCarpoolingService(CarpoolingRepository repository) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    // TODO CARPOOLING
    return null;
  }
}
