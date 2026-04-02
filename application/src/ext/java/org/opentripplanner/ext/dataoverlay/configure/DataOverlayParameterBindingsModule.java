package org.opentripplanner.ext.dataoverlay.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
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
  @Nullable
  static DataOverlayParameterBindings provideDataOverlayParameterBindings(BuildConfig config) {
    return config.dataOverlay != null ? config.dataOverlay.getParameterBindings() : null;
  }
}
