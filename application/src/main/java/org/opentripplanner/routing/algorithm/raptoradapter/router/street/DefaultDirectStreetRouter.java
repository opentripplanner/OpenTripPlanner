package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Generates "direct" street routes, i.e. those that do not use transit and are on the street
 * network for the entire itinerary. Doesn't support via locations or flex.
 */
public class DefaultDirectStreetRouter extends DirectStreetRouter {

  @Override
  List<GraphPath<State, Edge, Vertex>> findPaths(
    GraphPathFinder graphPathFinder,
    LinkingContext linkingContext,
    RouteRequest request
  ) {
    return List.of(graphPathFinder.graphPathFinderEntryPoint(request, linkingContext));
  }

  @Override
  boolean isRequestInvalidForRouting(RouteRequest request) {
    return request.journey().direct().mode() == StreetMode.NOT_SET;
  }

  @Override
  boolean isStraightLineDistanceWithinLimit(
    LinkingContext linkingContext,
    RouteRequest request,
    double maxDistanceLimit
  ) {
    // TODO This currently only calculates the distances between the first fromVertex
    //      and the first toVertex
    double distance = SphericalDistanceLibrary.distance(
      getFirstCoordinateForLocation(linkingContext, request.from()),
      getFirstCoordinateForLocation(linkingContext, request.to())
    );
    return distance < maxDistanceLimit;
  }
}
