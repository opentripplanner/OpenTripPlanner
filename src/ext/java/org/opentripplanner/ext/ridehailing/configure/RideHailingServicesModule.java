package org.opentripplanner.ext.ridehailing.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.ridehailing.RideHailingDepartureTimeShifter;
import org.opentripplanner.ext.ridehailing.RideHailingService;
import org.opentripplanner.ext.ridehailing.RideHailingServiceParameters;
import org.opentripplanner.ext.ridehailing.service.uber.UberService;
import org.opentripplanner.routing.service.RequestModifier;
import org.opentripplanner.standalone.config.RouterConfig;

/**
 * This module converts the ride hailing configurations into ride hailing services to be used by the
 * application context.
 */
@Module
public class RideHailingServicesModule {

  @Provides
  @Singleton
  List<RideHailingService> services(RouterConfig config) {
    return config
      .rideHailingServiceParameters()
      .stream()
      .map(p -> (RideHailingService) new UberService(p))
      .toList();
  }

  @Provides
  @Singleton
  RequestModifier requestModifier(List<RideHailingService> services) {
    if (services.isEmpty()) {
      return RequestModifier.NOOP;
    } else {
      return RideHailingDepartureTimeShifter::modifyRequest;
    }
  }
}
