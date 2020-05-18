package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

public class DirectStreetRouter {

  private static final Logger LOG = LoggerFactory.getLogger(DirectStreetRouter.class);

  /**
   * To avoid long searches witch might degrade the performance we use an upper limit
   * to the distance for none transit what we would allow.
   */
  private static final double MAX_WALK_DISTANCE_METERS =  50_000;
  private static final double MAX_BIKE_DISTANCE_METERS = 150_000;
  private static final double MAX_CAR_DISTANCE_METERS  = 500_000;

  public static List<Itinerary> route(Router router, RoutingRequest request) {
    request.setRoutingContext(router.graph);
    try {
      if (request.modes.directMode == null) {
        return Collections.emptyList();
      }
      if(!streetDistanceIsReasonable(request)) { return Collections.emptyList(); }

      RoutingRequest nonTransitRequest = request.getStreetSearchRequest(request.modes.directMode);

      // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
      GraphPathFinder gpFinder = new GraphPathFinder(router);
      List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(nonTransitRequest);

      // Convert the internal GraphPaths to itineraries
      List<Itinerary> response = GraphPathToItineraryMapper.mapItineraries(paths, request);
      ItinerariesHelper.decorateItinerariesWithRequestData(response, request);
      return response;
    }
    catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  private static boolean streetDistanceIsReasonable(RoutingRequest request) {
    // TODO This currently only calculates the distances between the first fromVertex
    //      and the first toVertex
    double distance = SphericalDistanceLibrary.distance(
        request.rctx.fromVertices
            .iterator()
            .next()
            .getCoordinate(),
        request.rctx.toVertices.iterator().next().getCoordinate()
    );
    return distance < calculateDistanceMaxLimit(request);
  }

  private static double calculateDistanceMaxLimit(RoutingRequest request) {
    double limit = request.maxWalkDistance * 2;
    double maxLimit = request.streetSubRequestModes.getCar()
        ? MAX_CAR_DISTANCE_METERS
        : (request.streetSubRequestModes.getBicycle() ? MAX_BIKE_DISTANCE_METERS : MAX_WALK_DISTANCE_METERS);

    // Handle overflow and default setting is set to Double MAX_VALUE
    // Everything above Long.MAX_VALUE is treated as Infinite
    if(limit< 0 || limit > Long.MAX_VALUE) {
      LOG.warn(
          "The max walk/bike/car distance is reduced to {} km from Infinite",
          (long)maxLimit/1000
      );
      return maxLimit;
    }

    if (limit > maxLimit) {
      LOG.warn(
          "The max walk/bike/car distance is reduced to {} km from {} km",
          (long)maxLimit/1000, (long)limit/1000
      );
      return maxLimit;
    }

    return limit;
  }
}
