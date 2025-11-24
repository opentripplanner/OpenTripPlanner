package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.LegsToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.StreetPathToLegsMapper;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.graphfinder.TransitServiceResolver;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.path.StreetPath;

/**
 * Generates "direct" street routes, i.e. those that do not use transit and are on the street
 * network for the entire itinerary. Doesn't support via locations or flex.
 */
public class DefaultDirectStreetRouter extends DirectStreetRouter {

  @Override
  List<Itinerary> findItineraries(
    OtpServerRequestContext serverContext,
    GraphPathFinder graphPathFinder,
    LinkingContext linkingContext,
    RouteRequest request
  ) {
    var paths = graphPathFinder.graphPathFinderEntryPoint(request, linkingContext);
    return mapToItineraries(serverContext, request, paths);
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

  private List<Itinerary> mapToItineraries(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    List<StreetPath> paths
  ) {
    StreetPathToLegsMapper streetPathToLegsMapper = new StreetPathToLegsMapper(
      new TransitServiceResolver(serverContext.transitService()),
      serverContext.transitService().getTimeZone(),
      serverContext.graph().streetNotesService,
      serverContext.streetDetailsService(),
      serverContext.graph().ellipsoidToGeoidDifference
    );
    List<Itinerary> itineraries = new ArrayList<>();
    for (var path : paths) {
      var legs = streetPathToLegsMapper.map(path, request);
      var itinerary = LegsToItineraryMapper.map(
        legs,
        path.lastState().isRentingVehicleFromStation(),
        path.calculateElevations()
      );
      itinerary.ifPresent(itineraries::add);
    }
    return itineraries;
  }
}
