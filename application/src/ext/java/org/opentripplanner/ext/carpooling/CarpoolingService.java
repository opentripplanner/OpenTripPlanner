package org.opentripplanner.ext.carpooling;

import java.time.ZonedDateTime;
import java.util.List;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.transit.service.TransitServiceResolver;

/**
 * Service for finding carpooling options by matching passenger requests with available driver trips.
 * <p>
 * Carpooling enables passengers to join existing driver journeys by being picked up and dropped off
 * along the driver's route. The service finds optimal insertion points for new passengers while
 * respecting capacity constraints, time windows, and route deviation budgets.
 */
public interface CarpoolingService {
  /**
   * Finds carpooling itineraries matching the passenger's routing request.
   *
   * @param request the routing request containing passenger origin, destination, and preferences
   * @return list of carpool itineraries, may be empty if no compatible trips found
   * @throws IllegalArgumentException if request is null
   */
  List<Itinerary> routeDirect(RouteRequest request);

  List<CarpoolAccessEgress> routeAccessEgress(
    RouteRequest request,
    StreetRequest streetRequest,
    AccessEgressType accessOrEgress,
    TransitServiceResolver transitServiceResolver,
    ZonedDateTime transitSearchTimeZero
  );
}
