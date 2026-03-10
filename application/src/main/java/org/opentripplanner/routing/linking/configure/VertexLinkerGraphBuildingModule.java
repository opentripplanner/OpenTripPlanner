package org.opentripplanner.routing.linking.configure;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.standalone.config.BuildConfig;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;

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
    return new VertexLinker(graph, mode, config.maxAreaNodes, OTPFeature.FlexRouting.isOn());
  }
}
