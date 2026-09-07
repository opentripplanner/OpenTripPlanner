package org.opentripplanner.ext.edgenaming.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.ext.edgenaming.EdgeNamerFactory;
import org.opentripplanner.graph_builder.module.osm.EdgeNamer;
import org.opentripplanner.standalone.config.BuildConfig;

@Module
public class EdgeNamerModule {

  @Provides
  @Singleton
  public static EdgeNamer provideNamer(BuildConfig config) {
    return EdgeNamerFactory.fromConfig(config.edgeNamer);
  }
}
