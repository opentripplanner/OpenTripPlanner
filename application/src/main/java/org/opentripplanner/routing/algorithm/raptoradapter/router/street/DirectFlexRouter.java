package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.ext.dataoverlay.configuration.DataOverlayParameterBindings;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.ext.flex.FlexParameters;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.ext.flex.filter.FilterMapper;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transfer.regular.RegularTransferService;
import org.opentripplanner.transit.service.TransitService;

public class DirectFlexRouter {

  public static List<Itinerary> route(
    Graph graph,
    TransitService transitService,
    RegularTransferService transferService,
    StreetDetailsService streetDetailsService,
    FlexParameters flexParameters,
    @Nullable DataOverlayParameterBindings dataOverlayParameterBindings,
    RouteRequest request,
    AdditionalSearchDays additionalSearchDays,
    LinkingContext linkingContext
  ) {
    if (!StreetMode.FLEXIBLE.equals(request.journey().direct().mode())) {
      return Collections.emptyList();
    }
    OTPRequestTimeoutException.checkForTimeout();
    // Prepare access/egress transfers
    Collection<NearbyStop> accessStops = AccessEgressRouter.findAccessEgresses(
      request,
      request.journey().direct().mode(),
      DataOverlayContext.listExtensionRequestContexts(request, dataOverlayParameterBindings),
      AccessEgressType.ACCESS,
      flexParameters.maxAccessWalkDuration(),
      0,
      linkingContext
    );
    Collection<NearbyStop> egressStops = AccessEgressRouter.findAccessEgresses(
      request,
      request.journey().direct().mode(),
      DataOverlayContext.listExtensionRequestContexts(request, dataOverlayParameterBindings),
      AccessEgressType.EGRESS,
      flexParameters.maxEgressWalkDuration(),
      0,
      linkingContext
    );

    var flexRouter = new FlexRouter(
      graph,
      transitService,
      transferService,
      streetDetailsService,
      flexParameters,
      FilterMapper.map(request.journey().transit().filters()),
      request.dateTime(),
      request.bookingTime(),
      additionalSearchDays.additionalSearchDaysInPast(),
      additionalSearchDays.additionalSearchDaysInFuture(),
      accessStops,
      egressStops
    );

    return new ArrayList<>(flexRouter.createFlexOnlyItineraries(request.arriveBy(), request));
  }
}
