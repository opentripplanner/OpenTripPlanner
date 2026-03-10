package org.opentripplanner.ext.gbfsgeofencing.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javax.annotation.Nullable;
import org.opentripplanner.ext.gbfsgeofencing.internal.graphbuilder.GbfsGeofencingGraphBuilder;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;

@Module
public class GbfsGeofencingGraphBuilderModule {

  @Provides
  @Singleton
  @Nullable
  static GbfsGeofencingGraphBuilder provideGbfsGeofencingGraphBuilder(
    BuildConfig config,
    Graph graph
  ) {
    if (!config.gbfsGeofencing.hasFeeds()) {
      return null;
    }

    return new GbfsGeofencingGraphBuilder(config.gbfsGeofencing, graph);
  }
}
