package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collections;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.StreetMode;

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
    if (isRequestInvalidForRouting(request)) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();

    var maxCarSpeed = serverContext.streetLimitationParametersService().maxCarSpeed();
    var maxDistanceLimit = calculateDistanceMaxLimit(request, maxCarSpeed);
    if (!isStraightLineDistanceWithinLimit(linkingContext, request, maxDistanceLimit)) {
      return Collections.emptyList();
    }

    try {
      // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
      GraphPathFinder gpFinder = new GraphPathFinder(
        serverContext.listExtensionRequestContexts(request),
        maxCarSpeed
      );
      var itineraries = findItineraries(serverContext, gpFinder, linkingContext, request);
      return decorateItineraries(request, itineraries);
    } catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  /**
   * Checks that the route request is configured to allow direct street results.
   */
  abstract boolean isRequestInvalidForRouting(RouteRequest request);

  /**
   * Checks that as the crow flies distance between locations in the search are within the maximum
   * distance limit.
   */
  abstract boolean isStraightLineDistanceWithinLimit(
    LinkingContext linkingContext,
    RouteRequest request,
    double maxDistanceLimit
  );

  /**
   * Find an ordered set of graph paths between the locations in the request starting from the
   * origin and ending in the destination. If there are no via locations, there is exactly one path.
   * With via locations, there is one path between each location.
   */
  abstract List<Itinerary> findItineraries(
    OtpServerRequestContext serverContext,
    GraphPathFinder graphPathFinder,
    LinkingContext linkingContext,
    RouteRequest request
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

  private static List<Itinerary> decorateItineraries(
    RouteRequest request,
    List<Itinerary> itineraries
  ) {
    return ItinerariesHelper.decorateItinerariesWithRequestData(
      itineraries,
      request.journey().wheelchair(),
      request.preferences().wheelchair()
    );
  }
}
