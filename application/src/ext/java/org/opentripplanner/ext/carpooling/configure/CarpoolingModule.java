package org.opentripplanner.ext.carpooling.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

@Module
public class CarpoolingModule {

  @Provides
  @Singleton
  public CarpoolingRepository provideCarpoolingRepository(Graph graph) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingRepository(graph);
  }

  @Provides
  public static CarpoolingService provideCarpoolingService(
    CarpoolingRepository repository,
    Graph graph
  ) {
    if (OTPFeature.CarPooling.isOff()) {
      return null;
    }
    return new DefaultCarpoolingService(repository, graph);
  }
}
