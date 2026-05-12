package org.opentripplanner.ext.dataoverlay.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import java.util.Optional;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.standalone.config.BuildConfig;

/**
 * Dagger module that provides DataOverlayParameterBindings from the build configuration. This
 * module is included in both the graph building and runtime Dagger components.
 */
@Module
public class DataOverlayParameterBindingsModule {

  @Provides
  @Singleton
  static Optional<DataOverlayParameterBindings> provideDataOverlayParameterBindings(
    BuildConfig config
  ) {
    return config.dataOverlay != null
      ? Optional.of(config.dataOverlay.getParameterBindings())
      : Optional.empty();
  }
}
