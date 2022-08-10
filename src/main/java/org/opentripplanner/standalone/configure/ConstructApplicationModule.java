package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import java.time.Duration;
import javax.inject.Singleton;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.api.StreetRoutingTimeout;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;


/**
 * Main focus is to provide bindings not provided elsewhere. For example binding specific config
 * parameters to DI managed input parameters. This allows us to keep avoid passing entire
 * configuration into modules, creating undecided dependencies.
 */
@Module
public class ConstructApplicationModule {

  @Provides
  @Singleton
  static RaptorConfig<TripSchedule> providesRaptorConfig(ConfigModel config) {
    return new RaptorConfig<>(config.routerConfig().raptorTuningParameters());
  }

  @Provides
  @StreetRoutingTimeout
  static Duration streetRoutingTimeout(ConfigModel config) {
    return config.routerConfig().streetRoutingTimeout();
  }
}
