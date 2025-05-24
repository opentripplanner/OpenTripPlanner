package org.opentripplanner.routing.linking.configure;

import static org.opentripplanner.routing.linking.VertexLinker.VisibilityMode.COMPUTE_AREA_VISIBILITY;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.StreetLimitationParameters;

/**
 * Provides the vertex linker for the routing application.
 */
@Module
public class VertexLinkerModule {

  @Provides
  static VertexLinker linker(Graph graph, StreetLimitationParameters params) {
    return new VertexLinker(graph, COMPUTE_AREA_VISIBILITY, params.maxAreaNodes());
  }
}
