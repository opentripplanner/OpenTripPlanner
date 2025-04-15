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
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;

/**
 * Generates "direct" street routes, i.e. those that do not use transit and are on the street
 * network for the entire itinerary.
 *
 * @see DirectFlexRouter
 */
public class DirectStreetRouter {

  public static List<Itinerary> route(OtpServerRequestContext serverContext, RouteRequest request) {
    if (request.journey().direct().mode() == StreetMode.NOT_SET) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();

    RouteRequest directRequest = request.clone();
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        serverContext.graph(),
        directRequest.from(),
        directRequest.to(),
        request.journey().direct().mode(),
        request.journey().direct().mode()
      )
    ) {
      var maxCarSpeed = serverContext.streetLimitationParametersService().getMaxCarSpeed();
      if (!straightLineDistanceIsWithinLimit(directRequest, temporaryVertices, maxCarSpeed)) {
        return Collections.emptyList();
      }

      // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
      GraphPathFinder gpFinder = new GraphPathFinder(
        serverContext.traverseVisitor(),
        serverContext.dataOverlayContext(request),
        maxCarSpeed
      );
      List<GraphPath<State, Edge, Vertex>> paths = gpFinder.graphPathFinderEntryPoint(
        directRequest,
        temporaryVertices
      );

      // Convert the internal GraphPaths to itineraries
      final GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
        serverContext.transitService().getTimeZone(),
        serverContext.graph().streetNotesService,
        serverContext.graph().ellipsoidToGeoidDifference
      );
      List<Itinerary> response = graphPathToItineraryMapper.mapItineraries(paths);
      response = ItinerariesHelper.decorateItinerariesWithRequestData(
        response,
        directRequest.wheelchair(),
        directRequest.preferences().wheelchair()
      );
      return response;
    } catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  private static boolean straightLineDistanceIsWithinLimit(
    RouteRequest request,
    TemporaryVerticesContainer vertexContainer,
    float maxCarSpeed
  ) {
    // TODO This currently only calculates the distances between the first fromVertex
    //      and the first toVertex
    double distance = SphericalDistanceLibrary.distance(
      vertexContainer.getFromVertices().iterator().next().getCoordinate(),
      vertexContainer.getToVertices().iterator().next().getCoordinate()
    );
    return distance < calculateDistanceMaxLimit(request, maxCarSpeed);
  }

  /**
   * Calculates the maximum distance in meters based on the maxDirectStreetDuration and the
   * fastest mode available. This assumes that it is not possible to exceed the speed defined in the
   * RouteRequest.
   */
  private static double calculateDistanceMaxLimit(RouteRequest request, float maxCarSpeed) {
    var preferences = request.preferences();
    double distanceLimit;
    StreetMode mode = request.journey().direct().mode();

    double durationLimit = preferences.street().maxDirectDuration().valueOf(mode).toSeconds();

    if (mode.includesDriving()) {
      distanceLimit = durationLimit * maxCarSpeed;
    } else if (mode.includesBiking()) {
      distanceLimit = durationLimit * preferences.bike().speed();
    } else if (mode.includesScooter()) {
      distanceLimit = durationLimit * preferences.scooter().speed();
    } else if (mode.includesWalking()) {
      distanceLimit = durationLimit * preferences.walk().speed();
    } else {
      throw new IllegalStateException("Could not set max limit for StreetMode");
    }

    return distanceLimit;
  }
}
