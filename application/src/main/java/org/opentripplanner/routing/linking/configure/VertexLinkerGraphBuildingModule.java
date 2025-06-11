package org.opentripplanner.routing.linking.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.linking.VisibilityMode;
import org.opentripplanner.standalone.config.BuildConfig;

/**
 * Provides the vertex linker for the graph build.
 */
@Module
public class VertexLinkerGraphBuildingModule {

  /**
   * The linker doesn't need to be a singleton as all state is kept in the graph.
   */
  @Provides
  @Singleton
  static VertexLinker linker(Graph graph, BuildConfig config) {
    var mode = VisibilityMode.ofBoolean(config.areaVisibility);
    return new VertexLinker(graph, mode, config.maxAreaNodes);
  }
}
