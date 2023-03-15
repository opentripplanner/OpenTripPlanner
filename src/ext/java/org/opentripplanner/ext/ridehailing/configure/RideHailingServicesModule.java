package org.opentripplanner.ext.ridehailing.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.util.List;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.ridehailing.service.RideHailingServiceParameters;
import org.opentripplanner.ext.ridehailing.service.uber.UberService;
import org.opentripplanner.standalone.config.RouterConfig;

@Module
public class RideHailingServicesModule {

  @Provides
  @Singleton
  List<RideHailingService> services(RouterConfig config) {
    return config
      .servicesParameters()
      .carHailingServiceParameters()
      .stream()
      .map(p -> {
        // in Java 21 this can hopefully use a switch statement
        if (p instanceof RideHailingServiceParameters.UberServiceParameters uberParams) {
          return (RideHailingService) new UberService(uberParams);
        } else {
          throw new IllegalArgumentException("Unknown car hailing params %s".formatted(p));
        }
      })
      .toList();
  }
}
