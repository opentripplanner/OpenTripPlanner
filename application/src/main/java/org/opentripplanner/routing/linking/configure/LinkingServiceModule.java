package org.opentripplanner.routing.linking.configure;

import static org.opentripplanner.routing.linking.VisibilityMode.COMPUTE_AREA_VISIBILITY_LINES;

import dagger.Module;
import dagger.Provides;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.TransitService;

@Module
public class LinkingServiceModule {

  @Provides
  static VertexLinker provideVertexLinker(
    Graph graph,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    return new VertexLinker(
      graph,
      COMPUTE_AREA_VISIBILITY_LINES,
      streetLimitationParametersService.maxAreaNodes()
    );
  }

  @Provides
  static VertexCreationService provideVertexCreationService(VertexLinker vertexLinker) {
    return new VertexCreationService(vertexLinker);
  }

  @Provides
  static LinkingContextFactory provideLinkingContextFactory(
    Graph graph,
    TransitService transitService,
    VertexCreationService vertexCreationService
  ) {
    return new LinkingContextFactory(
      graph,
      vertexCreationService,
      transitService::findStopOrChildIds
    );
  }
}
