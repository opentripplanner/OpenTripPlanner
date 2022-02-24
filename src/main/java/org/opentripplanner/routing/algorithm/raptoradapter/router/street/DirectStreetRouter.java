package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.api.request.StreetMode;
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

  public static List<Itinerary> route(Router router, RoutingRequest request) {
    if (request.modes.directMode == StreetMode.NOT_SET) {
      return Collections.emptyList();
    }

    try (RoutingRequest directRequest = request.getStreetSearchRequest(request.modes.directMode)) {
      directRequest.setRoutingContext(router.graph);

      if(!straightLineDistanceIsWithinLimit(directRequest)) { return Collections.emptyList(); }

      // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
      GraphPathFinder gpFinder = new GraphPathFinder(router);
      List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(directRequest);

      // Convert the internal GraphPaths to itineraries
      List<Itinerary> response = GraphPathToItineraryMapper.mapItineraries(paths);
      ItinerariesHelper.decorateItinerariesWithRequestData(response, directRequest);
      return response;
    }
    catch (PathNotFoundException e) {
      return Collections.emptyList();
    }
  }

  private static boolean straightLineDistanceIsWithinLimit(RoutingRequest request) {
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

  /**
   * Calculates the maximum distance in meters based on the maxDirectStreetDurationSeconds and
   * the fastest mode available. This assumes that it is not possible to exceed the speed defined
   * in the RoutingRequest.
   */
  private static double calculateDistanceMaxLimit(RoutingRequest request) {

    double distanceLimit;
    double durationLimit = request.maxDirectStreetDurationSeconds;
    StreetMode mode = request.modes.directMode;

    if (mode.includesDriving()) {
      distanceLimit = durationLimit * request.carSpeed;
    }
    else if (mode.includesBiking()) {
      distanceLimit = durationLimit * request.bikeSpeed;
    }
    else if (mode.includesWalking()) {
      distanceLimit = durationLimit * request.walkSpeed;
    }
    else {
      throw new IllegalStateException("Could not set max limit for StreetMode");
    }

    return distanceLimit;
  }
}
