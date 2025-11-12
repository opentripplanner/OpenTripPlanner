package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collections;
import java.util.List;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Generates "direct" street routes, i.e. those that do not use transit and are on the street
 * network for the entire itinerary. Doesn't support via locations or flex.
 */
public class DefaultDirectStreetRouter implements DirectStreetRouter {

  public List<Itinerary> route(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    LinkingContext linkingContext
  ) {
    if (request.journey().direct().mode() == StreetMode.NOT_SET) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();
    try {
      var maxCarSpeed = serverContext.streetLimitationParametersService().maxCarSpeed();
      if (!straightLineDistanceIsWithinLimit(request, maxCarSpeed, linkingContext)) {
        return Collections.emptyList();
      }

      // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
      GraphPathFinder gpFinder = new GraphPathFinder(
        serverContext.traverseVisitor(),
        serverContext.listExtensionRequestContexts(request),
        maxCarSpeed
      );
      List<GraphPath<State, Edge, Vertex>> paths = gpFinder.graphPathFinderEntryPoint(
        request,
        linkingContext
      );

      // Convert the internal GraphPaths to itineraries
      final GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
        serverContext.transitService()::getRegularStop,
        serverContext.transitService().getTimeZone(),
        serverContext.graph().streetNotesService,
        serverContext.graph().ellipsoidToGeoidDifference
      );
      List<Itinerary> response = graphPathToItineraryMapper.mapItineraries(paths);
      response = ItinerariesHelper.decorateItinerariesWithRequestData(
        response,
        request.journey().wheelchair(),
        request.preferences().wheelchair()
      );
      return response;
    } catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  private static boolean straightLineDistanceIsWithinLimit(
    RouteRequest request,
    float maxCarSpeed,
    LinkingContext linkingContext
  ) {
    // TODO This currently only calculates the distances between the first fromVertex
    //      and the first toVertex
    double distance = SphericalDistanceLibrary.distance(
      linkingContext.findVertices(request.from()).iterator().next().getCoordinate(),
      linkingContext.findVertices(request.to()).iterator().next().getCoordinate()
    );
    return distance < request.getMaximumDirectDistance(maxCarSpeed);
  }
}
