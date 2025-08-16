package org.opentripplanner.ext.carpooling;

import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

public interface CarpoolingService {
  List<Itinerary> route(OtpServerRequestContext serverContext, RouteRequest request)
    throws RoutingValidationException;
}
