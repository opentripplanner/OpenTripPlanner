package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Used to filter out unwanted access/egress legs. For walking and biking providing a lot of initial
 * states is not a problem, because most of them will quickly be dominated by other states. When
 * using modes that involve driving, however, we will get a lot more optimal states. This is because
 * driving is often much faster than transit, while also being configured to have a higher
 * generalized cost (to discourage driving over transit). This will lead to almost every
 * access/egress state to have a pareto optimal combination of time and cost, leading to much worse
 * performance for the Raptor search.
 */
public class AccessEgressFilter {

  static Collection<NearbyStop> filter(
      Collection<NearbyStop> nearbyStops, StreetMode streetMode, RoutingRequest request
  ) {
    switch (streetMode) {
      case CAR_PICKUP:
        return filterCarPickup(nearbyStops, request);
      case CAR_TO_PARK:
        return filterByCarPark(nearbyStops, request);
      default:
        return nearbyStops;
    }
  }

  /**
   * Returns the closest stops by distance
   */
  private static Collection<NearbyStop> filterCarPickup(
      Collection<NearbyStop> nearbyStops, RoutingRequest request
  ) {
    return nearbyStops
        .stream()
        .sorted()
        .limit(request.maxCarPickupAccessEgressStops)
        .collect(Collectors.toList());
  }

  /**
   * Returns the closest stops by distance
   */
  private static Collection<NearbyStop> filterByCarPark(
      Collection<NearbyStop> nearbyStops, RoutingRequest request
  ) {
    return nearbyStops
        .stream()
        .sorted()
        .limit(request.maxCarParkAccessEgressStops)
        .collect(Collectors.toList());
  }
}
