package org.opentripplanner.standalone.config.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.raptor.api.request.RaptorEnvironment;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.standalone.config.ConfigModel;
import org.opentripplanner.standalone.config.OtpConfig;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.config.routerconfig.RaptorEnvironmentFactory;

/**
 * Map {@link ConfigModel} into more specific types like {@link BuildConfig} to simplify
 * DI in other modules.
 */
@Module
public class ConfigModule {

  @Provides
  static OtpConfig provideOtpConfig(ConfigModel model) {
    return model.otpConfig();
  }

  @Provides
  static BuildConfig provideBuildConfig(ConfigModel model) {
    return model.buildConfig();
  }

  @Provides
  static RouterConfig provideRouterConfig(ConfigModel model) {
    return model.routerConfig();
  }

  @Provides
  @Singleton
  static RaptorConfig<TripSchedule> providesRaptorConfig(
    RouterConfig routerConfig,
    RaptorEnvironment environment
  ) {
    return new RaptorConfig<>(routerConfig.transitTuningConfig(), environment);
  }

  @Provides
  @Singleton
  static RaptorEnvironment providesRaptorEnvironment(RouterConfig routerConfig) {
    int searchThreadPoolSize = routerConfig.transitTuningConfig().searchThreadPoolSize();
    return RaptorEnvironmentFactory.create(searchThreadPoolSize);
  }
}
