package org.opentripplanner.routing.linking.configure;

import static org.opentripplanner.routing.linking.VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetLimitationParameters;

/**
 * Provides the vertex linker for the routing application (not graph building).
 */
@Module
public class VertexLinkerRoutingModule {

  @Provides
  @Singleton
  static VertexLinker linker(Graph graph, StreetLimitationParameters params) {
    return new VertexLinker(graph, COMPUTE_AREA_VISIBILITY_LINES, params.maxAreaNodes());
  }
}
