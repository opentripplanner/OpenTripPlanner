package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.api.request.RoutingRequestAndPreferences;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

public class DirectFlexRouter {

  public static List<Itinerary> route(
    OtpServerRequestContext serverContext,
    RoutingRequestAndPreferences opt,
    AdditionalSearchDays additionalSearchDays
  ) {
    if (!StreetMode.FLEXIBLE.equals(opt.request().journey().direct().mode())) {
      return Collections.emptyList();
    }
    var requestAndPreferences = opt
      .request()
      .getStreetSearchRequestAndPreferences(opt.request().journey().direct().mode(), opt);
    var directRequest = requestAndPreferences.request();
    var directPreferences = requestAndPreferences.preferences();

    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        serverContext.graph(),
        requestAndPreferences
      )
    ) {
      RoutingContext routingContext = new RoutingContext(
        requestAndPreferences,
        serverContext.graph(),
        temporaryVertices
      );

      // Prepare access/egress transfers
      Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
        routingContext,
        serverContext.transitService(),
        StreetMode.WALK,
        false
      );
      Collection<NearbyStop> egressStops = AccessEgressRouter.streetSearch(
        routingContext,
        serverContext.transitService(),
        StreetMode.WALK,
        true
      );

      FlexRouter flexRouter = new FlexRouter(
        serverContext.graph(),
        serverContext.transitService(),
        serverContext.routerConfig().flexParameters(directPreferences),
        directRequest.dateTime(),
        directRequest.arriveBy(),
        additionalSearchDays.additionalSearchDaysInPast(),
        additionalSearchDays.additionalSearchDaysInFuture(),
        accessStops,
        egressStops
      );

      return new ArrayList<>(flexRouter.createFlexOnlyItineraries());
    }
  }
}
