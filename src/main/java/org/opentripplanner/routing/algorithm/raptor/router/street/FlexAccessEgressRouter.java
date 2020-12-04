package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Collection;
import java.util.List;

public class FlexAccessEgressRouter {

  private FlexAccessEgressRouter() {}

  public static Collection<FlexAccessEgress> routeAccessEgress(
      RoutingRequest request, boolean isEgress, int additionalPastSearchDays,
      int additionalFutureSearchDays
  ) {

    Collection<NearbyStop> accessStops = !isEgress ? AccessEgressRouter.streetSearch(request,
        StreetMode.WALK,
        false,
        2000
    ) : List.of();

    Collection<NearbyStop> egressStops = isEgress ? AccessEgressRouter.streetSearch(request,
        StreetMode.WALK,
        true,
        2000
    ) : List.of();

    FlexRouter flexRouter = new FlexRouter(request.rctx.graph,
        request.getDateTime().toInstant(),
        request.arriveBy,
        additionalPastSearchDays,
        additionalFutureSearchDays,
        accessStops,
        egressStops
    );

    return isEgress ? flexRouter.createFlexEgresses() : flexRouter.createFlexAccesses();
  }
}
