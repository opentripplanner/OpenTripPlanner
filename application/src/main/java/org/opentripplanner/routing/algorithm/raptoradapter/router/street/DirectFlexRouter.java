package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.ext.flex.filter.FilterMapper;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.FromToViaVertexRequest;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

public class DirectFlexRouter {

  public static List<Itinerary> route(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    AdditionalSearchDays additionalSearchDays,
    FromToViaVertexRequest fromToViaVertexRequest
  ) {
    if (!StreetMode.FLEXIBLE.equals(request.journey().direct().mode())) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();
    // Prepare access/egress transfers
    Collection<NearbyStop> accessStops = AccessEgressRouter.findAccessEgresses(
      request,
      request.journey().direct(),
      serverContext.dataOverlayContext(request),
      AccessEgressType.ACCESS,
      serverContext.flexParameters().maxAccessWalkDuration(),
      0,
      fromToViaVertexRequest
    );
    Collection<NearbyStop> egressStops = AccessEgressRouter.findAccessEgresses(
      request,
      request.journey().direct(),
      serverContext.dataOverlayContext(request),
      AccessEgressType.EGRESS,
      serverContext.flexParameters().maxEgressWalkDuration(),
      0,
      fromToViaVertexRequest
    );

    var flexRouter = new FlexRouter(
      serverContext.graph(),
      serverContext.transitService(),
      serverContext.flexParameters(),
      FilterMapper.map(request.journey().transit().filters()),
      request.dateTime(),
      request.bookingTime(),
      additionalSearchDays.additionalSearchDaysInPast(),
      additionalSearchDays.additionalSearchDaysInFuture(),
      accessStops,
      egressStops
    );

    return new ArrayList<>(flexRouter.createFlexOnlyItineraries(request.arriveBy()));
  }
}
