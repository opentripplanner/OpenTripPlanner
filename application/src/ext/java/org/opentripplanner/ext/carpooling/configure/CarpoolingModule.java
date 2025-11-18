package org.opentripplanner.ext.carpooling.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.service.DefaultCarpoolingService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;

@Module
public class CarpoolingModule {

  @Provides
  @Singleton
  @Nullable
  public CarpoolingRepository provideCarpoolingRepository() {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingRepository();
  }

  @Provides
  @Nullable
  public static CarpoolingService provideCarpoolingService(
    @Nullable CarpoolingRepository repository,
    StreetLimitationParametersService streetLimitationParametersService,
    TransitService transitService,
    VertexLinker vertexLinker
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingService(
      repository,
      streetLimitationParametersService,
      transitService,
      vertexLinker
    );
  }
}
