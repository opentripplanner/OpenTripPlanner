package org.opentripplanner.ext.carhailing.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.util.List;
import org.opentripplanner.ext.carhailing.CarHailingService;
import org.opentripplanner.ext.carhailing.service.CarHailingServiceParameters;
import org.opentripplanner.ext.carhailing.service.uber.UberService;
import org.opentripplanner.standalone.config.RouterConfig;

@Module
public class CarHailingServicesModule {

  @Provides
  @Singleton
  List<CarHailingService> services(RouterConfig config) {
    return config
      .servicesParameters()
      .carHailingServiceParameters()
      .stream()
      .map(p -> {
        // in Java 21 this can hopefully use a switch statement
        if (p instanceof CarHailingServiceParameters.UberServiceParameters uberParams) {
          return (CarHailingService) new UberService(uberParams);
        } else {
          throw new IllegalArgumentException("Unknown car hailing params %s".formatted(p));
        }
      })
      .toList();
  }
}
