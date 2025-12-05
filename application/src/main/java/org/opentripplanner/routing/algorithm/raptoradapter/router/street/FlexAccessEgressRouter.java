package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.ext.flex.filter.FilterMapper;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.ExtensionRequestContext;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

public class FlexAccessEgressRouter {

  private FlexAccessEgressRouter() {}

  public static Collection<FlexAccessEgress> routeAccessEgress(
    RouteRequest request,
    TransitService transitService,
    Graph graph,
    RegularTransferService transferService,
    GeofencingZoneService geofencingZoneService,
    StreetLimitationParametersService streetLimitationParametersService,
    StreetDetailsService streetDetailsService,
    AccessEgressRouter accessEgressRouter,
    AdditionalSearchDays searchDays,
    FlexParameters config,
    Collection<ExtensionRequestContext> extensionRequestContexts,
    AccessEgressType accessOrEgress,
    LinkingContext linkingContext
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    Collection<NearbyStop> accessStops = accessOrEgress.isAccess()
      ? accessEgressRouter.findAccessEgresses(
          request,
          StreetMode.WALK,
          extensionRequestContexts,
          AccessEgressType.ACCESS,
          config.maxAccessWalkDuration(),
          0,
          linkingContext,
          streetLimitationParametersService,
          geofencingZoneService
        )
      : List.of();

    Collection<NearbyStop> egressStops = accessOrEgress.isEgress()
      ? accessEgressRouter.findAccessEgresses(
          request,
          StreetMode.WALK,
          extensionRequestContexts,
          AccessEgressType.EGRESS,
          config.maxEgressWalkDuration(),
          0,
          linkingContext,
          streetLimitationParametersService,
          geofencingZoneService
        )
      : List.of();

    FlexRouter flexRouter = new FlexRouter(
      graph,
      transitService,
      transferService,
      streetDetailsService,
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
