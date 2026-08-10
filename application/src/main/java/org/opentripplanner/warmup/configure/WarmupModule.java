package org.opentripplanner.warmup.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.standalone.config.RouterConfig;
import org.opentripplanner.standalone.configure.RequestScopedFactory;
import org.opentripplanner.transit.service.TransitRepository;
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
    RequestScopedFactory.Builder requestScopedComponentBuilder,
    TransitRepository transitRepository
  ) {
    return new WarmupLauncher(
      parameters,
      () -> requestScopedComponentBuilder.build().createServerContext(),
      transitRepository
    );
  }
}
