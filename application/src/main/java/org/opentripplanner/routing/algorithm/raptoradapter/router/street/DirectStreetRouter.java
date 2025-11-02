package org.opentripplanner.routing.algorithm.raptoradapter.router.street;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

/**
 * Generates "direct" street routes, i.e. those that do not use transit and are on the street
 * network for the entire itinerary. For flex routing, use {@link DirectFlexRouter}.
 */
public interface DirectStreetRouter {
  List<Itinerary> route(
    OtpServerRequestContext serverContext,
    RouteRequest request,
    LinkingContext linkingContext
  );
}
