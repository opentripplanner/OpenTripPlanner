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
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.service.StreetLimitationParametersService;

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
    Graph graph,
    VertexLinker vertexLinker,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingService(
      repository,
      graph,
      vertexLinker,
      streetLimitationParametersService
    );
  }
}
