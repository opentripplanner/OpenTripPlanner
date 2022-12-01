package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TemporaryVerticesContainer;

public class DirectFlexRouter {

  public static List<Itinerary> route(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    AdditionalSearchDays additionalSearchDays
  ) {
    if (!StreetMode.FLEXIBLE.equals(request.journey().direct().mode())) {
      return Collections.emptyList();
    }
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        serverContext.graph(),
        request,
        request.journey().direct().mode(),
        request.journey().direct().mode()
      )
    ) {
      // Prepare access/egress transfers
      Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
        request,
        temporaryVertices,
        serverContext.transitService(),
        request.journey().direct(),
        serverContext.dataOverlayContext(request),
        false
      );
      Collection<NearbyStop> egressStops = AccessEgressRouter.streetSearch(
        request,
        temporaryVertices,
        serverContext.transitService(),
        request.journey().direct(),
        serverContext.dataOverlayContext(request),
        true
      );

      FlexRouter flexRouter = new FlexRouter(
        serverContext.graph(),
        serverContext.transitService(),
        serverContext.flexConfig(),
        request.dateTime(),
        request.arriveBy(),
        request.preferences().walk().speed(),
        additionalSearchDays.additionalSearchDaysInPast(),
        additionalSearchDays.additionalSearchDaysInFuture(),
        accessStops,
        egressStops
      );

      return new ArrayList<>(flexRouter.createFlexOnlyItineraries());
    }
  }
}
