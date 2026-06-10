package org.opentripplanner.warmup.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.warmup.WarmupLauncher;
import org.opentripplanner.warmup.api.WarmupParameters;

/**
 * Dagger wiring for the application warmup feature.
 * <p>
 * Provides the {@link WarmupParameters} binding (mapped from the JSON config section by {@code
 * WarmupConfig}) and the {@link WarmupLauncher} that {@link
 * org.opentripplanner.standalone.configure.ConstructApplication} uses to start the warmup thread
 * after Raptor transit data and updaters have been set up.
 */
@Module
public class WarmupModule {

  @Provides
  @Nullable
  static WarmupParameters provideWarmupParameters(RouterConfig routerConfig) {
    return routerConfig.warmupParameters();
  }

  @Provides
  @Singleton
  static WarmupLauncher provideWarmupLauncher(
    @Nullable WarmupParameters parameters,
    OtpServerRequestContext serverContext,
    TimetableRepository timetableRepository
  ) {
    return new WarmupLauncher(parameters, serverContext, timetableRepository);
  }
}
