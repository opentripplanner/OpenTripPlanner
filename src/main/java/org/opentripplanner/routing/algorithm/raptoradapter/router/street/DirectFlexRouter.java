package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.server.Router;

public class DirectFlexRouter {

  public static List<Itinerary> route(
      Router router,
      RoutingRequest request,
      AdditionalSearchDays additionalSearchDays
  ) {
    if (!StreetMode.FLEXIBLE.equals(request.modes.directMode)) {
      return Collections.emptyList();
    }

    try (RoutingRequest directRequest = request.getStreetSearchRequest(request.modes.directMode)) {
      directRequest.setRoutingContext(router.graph);

      // Prepare access/egress transfers
      Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
              directRequest,
              StreetMode.WALK,
              false
      );
      Collection<NearbyStop> egressStops = AccessEgressRouter.streetSearch(
              directRequest,
              StreetMode.WALK,
              true
      );

      FlexRouter flexRouter = new FlexRouter(
              router.graph,
              router.routerConfig.flexParameters(request),
              directRequest.getDateTime(),
              directRequest.arriveBy,
              additionalSearchDays.additionalSearchDaysInPast(),
              additionalSearchDays.additionalSearchDaysInFuture(),
              accessStops,
              egressStops
      );

      return new ArrayList<>(flexRouter.createFlexOnlyItineraries());
    }
  }
}
