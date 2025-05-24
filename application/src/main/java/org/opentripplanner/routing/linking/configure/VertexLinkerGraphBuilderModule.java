package org.opentripplanner.routing.linking.configure;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.standalone.config.BuildConfig;

/**
 * Provides the vertex linker for the graph build.
 */
@Module
public class VertexLinkerGraphBuilderModule {

  /**
   * The linker doesn't need to be a singleton as all state is kept in the graph.
   */
  @Provides
  static VertexLinker linker(Graph graph, BuildConfig config) {
    var mode = VertexLinker.VisibilityMode.ofBoolean(config.areaVisibility);
    return new VertexLinker(graph, mode, config.maxAreaNodes);
  }
}
