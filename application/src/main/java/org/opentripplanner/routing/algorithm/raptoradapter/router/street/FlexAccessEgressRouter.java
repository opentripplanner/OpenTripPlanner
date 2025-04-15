package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.ext.flex.filter.FilterMapper;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.transit.service.TransitService;

public class FlexAccessEgressRouter {

  private FlexAccessEgressRouter() {}

  public static Collection<FlexAccessEgress> routeAccessEgress(
    RouteRequest request,
    TemporaryVerticesContainer verticesContainer,
    OtpServerRequestContext serverContext,
    AdditionalSearchDays searchDays,
    FlexParameters config,
    DataOverlayContext dataOverlayContext,
    AccessEgressType accessOrEgress
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    TransitService transitService = serverContext.transitService();

    Collection<NearbyStop> accessStops = accessOrEgress.isAccess()
      ? AccessEgressRouter.findAccessEgresses(
        request,
        verticesContainer,
        new StreetRequest(StreetMode.WALK),
        dataOverlayContext,
        AccessEgressType.ACCESS,
        serverContext.flexParameters().maxAccessWalkDuration(),
        0
      )
      : List.of();

    Collection<NearbyStop> egressStops = accessOrEgress.isEgress()
      ? AccessEgressRouter.findAccessEgresses(
        request,
        verticesContainer,
        new StreetRequest(StreetMode.WALK),
        dataOverlayContext,
        AccessEgressType.EGRESS,
        serverContext.flexParameters().maxEgressWalkDuration(),
        0
      )
      : List.of();

    FlexRouter flexRouter = new FlexRouter(
      serverContext.graph(),
      transitService,
      config,
      FilterMapper.map(request.journey().transit().filters()),
      request.dateTime(),
      request.bookingTime(),
      searchDays.additionalSearchDaysInPast(),
      searchDays.additionalSearchDaysInFuture(),
      accessStops,
      egressStops
    );

    return accessOrEgress.isEgress()
      ? flexRouter.createFlexEgresses()
      : flexRouter.createFlexAccesses();
  }
}
