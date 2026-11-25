package org.opentripplanner.ext.carpooling;

import java.time.ZonedDateTime;
import java.util.List;
import org.opentripplanner.ext.carpooling.mapping.CarpoolAccessEgress;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.transit.service.TransitServiceResolver;

/**
 * Service for finding carpooling options by matching passenger requests with available driver
 * trips.
 * <p>
 * Carpooling enables passengers to join existing driver journeys by being picked up and dropped
 * off along the driver's route. The service finds optimal insertion points for new passengers while
 * respecting capacity constraints, time windows, and route deviation budgets.
 */
public interface CarpoolingService {
  /**
   * Finds carpool itineraries from the request's origin to its destination.
   *
   * @return the itineraries; empty unless the direct mode is {@code CARPOOL}
   * @throws RoutingValidationException if the origin or the destination has no coordinates
   */
  List<Itinerary> routeDirect(RouteRequest request);

  /**
   * Finds carpool rides between the passenger and transit stops: from the origin to stops for
   * access, from stops to the destination for egress.
   *
   * @param transitSearchTimeZero the time the returned legs' times are relative to
   * @return the rides; empty unless the access or egress mode is {@code CARPOOL}
   * @throws RoutingValidationException if the origin or the destination has no coordinates
   */
  List<CarpoolAccessEgress> routeAccessEgress(
    RouteRequest request,
    AccessEgressType accessOrEgress,
    TransitServiceResolver transitServiceResolver,
    ZonedDateTime transitSearchTimeZero
  );
}
