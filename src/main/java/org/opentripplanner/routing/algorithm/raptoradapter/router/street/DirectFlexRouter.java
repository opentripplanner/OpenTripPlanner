package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.transit.service.TransitService;

public class DirectFlexRouter {

  private final OtpServerRequestContext serverContext;
  private final TransitService transitService;

  public DirectFlexRouter(OtpServerRequestContext serverContext, TransitService transitService) {
    this.serverContext = serverContext;
    this.transitService = transitService;
  }

  public List<Itinerary> route(RouteRequest request, AdditionalSearchDays additionalSearchDays) {
    if (!StreetMode.FLEXIBLE.equals(request.journey().direct().mode())) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();
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
        transitService,
        request.journey().direct(),
        serverContext.dataOverlayContext(request),
        false,
        serverContext.flexConfig().maxAccessWalkDuration(),
        0
      );
      Collection<NearbyStop> egressStops = AccessEgressRouter.streetSearch(
        request,
        temporaryVertices,
        transitService,
        request.journey().direct(),
        serverContext.dataOverlayContext(request),
        true,
        serverContext.flexConfig().maxEgressWalkDuration(),
        0
      );

      FlexRouter flexRouter = new FlexRouter(
        serverContext.graph(),
        transitService,
        serverContext.flexConfig(),
        request.dateTime(),
        request.arriveBy(),
        additionalSearchDays.additionalSearchDaysInPast(),
        additionalSearchDays.additionalSearchDaysInFuture(),
        accessStops,
        egressStops
      );

      return new ArrayList<>(flexRouter.createFlexOnlyItineraries());
    }
  }
}
