package org.opentripplanner.ext.carpooling;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.RoutingValidationException;

public interface CarpoolingService {
  List<Itinerary> route(RouteRequest request)
    throws RoutingValidationException;
}
