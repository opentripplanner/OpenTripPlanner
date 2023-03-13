package org.opentripplanner.ext.carhailing.configure;

import dagger.Module;
import dagger.Provides;
import java.util.List;
import org.opentripplanner.ext.carhailing.service.CarHailingService;
import org.opentripplanner.ext.carhailing.service.uber.UberService;
import org.opentripplanner.ext.carhailing.service.uber.UberServiceParameters;
import org.opentripplanner.standalone.config.RouterConfig;

@Module
public class CarHailingServicesModule {

  @Provides
  List<CarHailingService> services(RouterConfig config) {
    return config
      .servicesParameters()
      .carHailingServiceParameters()
      .stream()
      .map(p -> {
        if (p instanceof UberServiceParameters uberParams) {
          return (CarHailingService) new UberService(uberParams);
        } else {
          throw new IllegalArgumentException("Unknown car hailing params %s".formatted(p));
        }
      })
      .toList();
  }
}
