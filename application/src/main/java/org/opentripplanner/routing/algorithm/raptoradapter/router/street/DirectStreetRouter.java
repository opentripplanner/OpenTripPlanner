package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collections;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.graphfinder.TransitServiceResolver;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;

/**
 * Abstract class for generating "direct" street routes, i.e. those that do not use transit and are
 * on the street network for the entire itinerary. For flex routing, use {@link DirectFlexRouter}.
 * Follows template method pattern.
 */
public abstract class DirectStreetRouter {

  /**
   * @return direct street itineraries.
   */
  public List<Itinerary> route(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    LinkingContext linkingContext
  ) {
    if (isRequestValidForRouting(request)) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();

    var maxCarSpeed = serverContext.streetLimitationParametersService().maxCarSpeed();
    var maxDistanceLimit = calculateDistanceMaxLimit(request, maxCarSpeed);
    if (!isStraightLineDistanceIsWithinLimit(linkingContext, request, maxDistanceLimit)) {
      return Collections.emptyList();
    }

    try {
      var paths = findPaths(serverContext, linkingContext, request, maxCarSpeed);
      return mapToItineraries(serverContext, request, paths);
    } catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Checks that the route request is configured to allow direct street results.
   */
  abstract boolean isRequestValidForRouting(RouteRequest request);

  /**
   * Checks that as the crow flies distance between locations in the search are within the maximum
   * distance limit.
   */
  abstract boolean isStraightLineDistanceIsWithinLimit(
    LinkingContext linkingContext,
    RouteRequest request,
    double maxDistanceLimit
  );

  /**
   * Find graph paths between the locations in the request.
   */
  abstract List<GraphPath<State, Edge, Vertex>> findPaths(
    OtpServerRequestContext serverContext,
    LinkingContext linkingContext,
    RouteRequest request,
    float maxCarSpeed
  );

  static Coordinate getFirstCoordinateForLocation(
    LinkingContext context,
    GenericLocation location
  ) {
    return context.findVertices(location).iterator().next().getCoordinate();
  }

  /**
   * Calculates the maximum distance in meters based on the maxDirectStreetDuration and the
   * fastest mode available. This assumes that it is not possible to exceed the speed defined in the
   * RouteRequest.
   */
  private static double calculateDistanceMaxLimit(RouteRequest request, float maxCarSpeed) {
    var preferences = request.preferences();
    StreetMode mode = request.journey().direct().mode();

    double durationLimit = preferences.street().maxDirectDuration().valueOf(mode).toSeconds();

    if (mode.includesDriving()) {
      return durationLimit * maxCarSpeed;
    }
    if (mode.includesBiking()) {
      return durationLimit * preferences.bike().speed();
    }
    if (mode.includesScooter()) {
      return durationLimit * preferences.scooter().speed();
    }
    if (mode.includesWalking()) {
      return durationLimit * preferences.walk().speed();
    }
    throw new IllegalStateException("Could not set max limit for StreetMode");
  }

  private static List<Itinerary> mapToItineraries(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    List<GraphPath<State, Edge, Vertex>> paths
  ) {
    final GraphPathToItineraryMapper graphPathToItineraryMapper = new GraphPathToItineraryMapper(
      new TransitServiceResolver(serverContext.transitService()),
      serverContext.transitService().getTimeZone(),
      serverContext.graph().streetNotesService,
      serverContext.streetDetailsService(),
      serverContext.graph().ellipsoidToGeoidDifference
    );
    List<Itinerary> response = graphPathToItineraryMapper.mapItineraries(paths, request);
    return ItinerariesHelper.decorateItinerariesWithRequestData(
      response,
      request.journey().wheelchair(),
      request.preferences().wheelchair()
    );
  }
}
