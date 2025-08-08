package org.opentripplanner.ext.carpooling.internal;

import java.util.List;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.RoutingValidationException;

public class DefaultCarpoolingService implements CarpoolingService {

  /**
   * TERMINOLOGY
   * - Boarding and alighting area stops
   *
   *
   * ALGORITHM OUTLINE
   *
   * <pre>
   *   DIRECT_DISTANCE = SphericalDistanceLibrary.fastDistance(fromLocation, toLocation)
   *   // 3000m is about 45 minutes of walking
   *   MAX_WALK_DISTANCE = max(DIRECT_DISTANCE, 3000m)
   *   MAX_COST = DIRECT_DISTANCE * walkReluctance
   *
   * Search for access / egress candidates (AreaStops) using
   * - accessDistance = SphericalDistanceLibrary.fastDistance(fromLocation, stop.center);
   * - Drop candidates where accessDistance greater then MAX_WALK_DISTANCE and is not within time constraints
   * - egressDistance = SphericalDistanceLibrary.fastDistance(toLocation, stop.center);
   * - Drop candidates where (accessDistance + egressDistance) greater then MAX_WALK_DISTANCE (no time check)
   * - Sort candidates on estimated cost, where we use direct distance instead of actual distance
   *
   * FOR EACH CANDIDATE (C)
   * - Use AStar to find the actual distance for:
   *   - access path
   *   - transit path
   *   - egress path
   * - Drop candidates where (access+carpool+egress) cost > MAX_COST
   * [- Abort when no more optimal results can be obtained (pri2)]
   *
   * Create Itineraries for the top 3 results and return
   * </pre>
   */
  @Override
  public List<Itinerary> route(RouteRequest request) throws RoutingValidationException {
    return List.of();
  }
}
