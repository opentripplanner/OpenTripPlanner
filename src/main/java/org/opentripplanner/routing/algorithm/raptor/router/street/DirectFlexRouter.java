package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.util.OTPFeature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DirectFlexRouter {

  public static List<Itinerary> route(
      RoutingRequest request,
      int additionalPastSearchDays,
      int additionalFutureSearchDays
  ) {
    if (!StreetMode.FLEXIBLE.equals(request.modes.directMode)) {
      return Collections.emptyList();
    }

    // Prepare access/egress transfers
    Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
        request,
        StreetMode.WALK,
        false,
        2000
    );
    Collection<NearbyStop> egressStops = AccessEgressRouter.streetSearch(
        request,
        StreetMode.WALK,
        true,
        2000
    );

    FlexRouter flexRouter = new FlexRouter(
        request.rctx.graph,
        request.getDateTime().toInstant(),
        request.arriveBy,
        additionalPastSearchDays,
        additionalFutureSearchDays,
        accessStops,
        egressStops
    );

    return new ArrayList<>(flexRouter.createFlexOnlyItineraries());
  }
}
