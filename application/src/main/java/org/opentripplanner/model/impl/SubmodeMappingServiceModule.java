package org.opentripplanner.model.impl;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.routing.graph.Graph;

@Module
public class SubmodeMappingServiceModule {

  @Provides
  @Singleton
  public static SubmodeMappingService submodeMappingService(Graph graph) {
    return new SubmodeMappingService(graph.submodeMapping);
  }
}
