package org.opentripplanner.standalone.configure;

import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.raptor.configure.RaptorConfig;

@Module
class AppModule {

  @Provides
  @Singleton
  static RaptorConfig<TripSchedule> providesRaptorConfig(RouterConfig routerConfig) {
    return new RaptorConfig<>(routerConfig.raptorTuningParameters());
  }
}
