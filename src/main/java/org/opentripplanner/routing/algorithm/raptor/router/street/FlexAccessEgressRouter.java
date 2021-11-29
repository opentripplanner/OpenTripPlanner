package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexParameters;

public class FlexAccessEgressRouter {

  private FlexAccessEgressRouter() {}

  public static Collection<FlexAccessEgress> routeAccessEgress(
      RoutingRequest request,
      FlexParameters params,
      boolean isEgress
  ) {

    Collection<NearbyStop> accessStops = !isEgress ? AccessEgressRouter.streetSearch(
        request,
        StreetMode.WALK,
        false
    ) : List.of();

    Collection<NearbyStop> egressStops = isEgress ? AccessEgressRouter.streetSearch(
        request,
        StreetMode.WALK,
        true
    ) : List.of();

    FlexRouter flexRouter = new FlexRouter(
        request.rctx.graph,
        params,
        request.getDateTime().toInstant(),
        request.arriveBy,
        request.additionalSearchDaysBeforeToday,
        request.additionalSearchDaysAfterToday,
        accessStops,
        egressStops
    );

    return isEgress ? flexRouter.createFlexEgresses() : flexRouter.createFlexAccesses();
  }
}
