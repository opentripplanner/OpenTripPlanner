package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import org.opentripplanner.astar.spi.TraverseVisitor;
import org.opentripplanner.graph_builder.module.nearbystops.StopResolver;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * This uses a street search to find paths to all the access/egress stop within range. Doesn't
 * support routing through via locations.
 */
public class DefaultAccessEgressRouter extends AccessEgressRouter {

  private final StopResolver stopResolver;

  DefaultAccessEgressRouter(StopResolver stopResolver) {
    super(stopResolver);
    this.stopResolver = stopResolver;
  }

  @Override
  Collection<NearbyStop> findStreetAccessEgresses(
    RouteRequest request,
    StreetMode streetMode,
    TraverseVisitor<State, Edge> traverseVisitor,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    AccessEgressType accessOrEgress,
    Duration durationLimit,
    int maxStopCount,
    LinkingContext linkingContext,
    Set<Vertex> ignoreVertices,
    float maxCarSpeed
  ) {
    var originVertices = accessOrEgress.isAccess()
      ? linkingContext.findVertices(request.from())
      : linkingContext.findVertices(request.to());
    return StreetNearbyStopFinder.of(stopResolver, durationLimit, maxStopCount)
      .withIgnoreVertices(ignoreVertices)
      .withExtensionRequestContexts(extensionRequestContexts)
      .build()
      .findNearbyStops(originVertices, request, streetMode, accessOrEgress.isEgress());
  }
}
