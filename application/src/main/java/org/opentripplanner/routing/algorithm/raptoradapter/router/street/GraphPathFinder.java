package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;

/// A helper class for finding paths through the street graph.
///
/// It links the from/to, sets up the [StreetSearchBuilder], executes it,
/// and throws an exception if no path is found.
class GraphPathFinder {

  private final Collection<ExtensionRequestContext> extensionRequestContexts;

  private final float maxCarSpeed;

  GraphPathFinder(Collection<ExtensionRequestContext> extensionRequestContexts, float maxCarSpeed) {
    this.extensionRequestContexts = Objects.requireNonNull(extensionRequestContexts);
    this.maxCarSpeed = maxCarSpeed;
  }

  List<StreetPath> find(RouteRequest request, LinkingContext linkingContext) {
    Set<Vertex> from = linkingContext.findVertices(request.from());
    Set<Vertex> to = linkingContext.findVertices(request.to());
    OTPRequestTimeoutException.checkForTimeout();

    var paths = findPaths(request, from, to);

    if (paths.isEmpty()) {
      throw new PathNotFoundException();
    }

    return paths;
  }

  private List<StreetPath> findPaths(RouteRequest request, Set<Vertex> from, Set<Vertex> to) {
    StreetPreferences preferences = request.preferences().street();

    StreetSearchBuilder streetSearch = StreetSearchBuilder.of()
      .withPreStartHook(OTPRequestTimeoutException::checkForTimeout)
      .withHeuristic(new EuclideanRemainingWeightHeuristic(maxCarSpeed))
      .withSkipEdgeStrategy(
        new DurationSkipEdgeStrategy(
          preferences.maxDirectDuration().valueOf(request.journey().direct().mode())
        )
      )
      // FORCING the dominance function to weight only
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withRequest(
        StreetSearchRequestMapper.map(request)
          .withExtensionRequestContexts(extensionRequestContexts)
          .withMode(request.journey().direct().mode())
          .build()
      )
      .withFrom(from)
      .withTo(to);

    return streetSearch.getPathsToTarget();
  }
}
