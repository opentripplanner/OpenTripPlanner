package org.opentripplanner.routing.algorithm.raptor.router.street;

import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.edgetype.ParkAndRideEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Collection;
import java.util.Set;
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
        return filterCarPickup(nearbyStops, request.maxCarPickupAccessEgressStops);
      case CAR_TO_PARK:
        return filterByCarPark(nearbyStops, request.maxCarParkAccessEgressStops);
      default:
        return nearbyStops;
    }
  }

  /**
   * Returns the closest stops by distance. In addition, all other stops within the same stations
   * as the closest stops are added.
   */
  private static Collection<NearbyStop> filterCarPickup(
      Collection<NearbyStop> nearbyStops, int maxCarPickupAccessEgressStops
  ) {
    // Add all stops locations (including areas) within range to result
    Set<NearbyStop> result = nearbyStops.stream()
        // Sorts by least distance
        .sorted()
        .limit(maxCarPickupAccessEgressStops)
        .collect(Collectors.toSet());

    // Get the station for the stops within range
    Set<Station> stations = result
        .stream()
        .filter(s -> s.stop instanceof Stop)
        .map(s -> ((Stop) s.stop).getParentStation())
        .collect(Collectors.toSet());

    // All the child stops that are outside of the range
    result.addAll(
        nearbyStops
        .stream()
        .filter(s -> s.stop instanceof Stop)
        .filter(s -> stations.contains(((Stop) s.stop).getParentStation()))
        .collect(Collectors.toList())
    );

    return result;
  }

  /**
   * Returns the closest stops by distance. In addition, all other stops using the same car park
   * are added.
   */
  private static Collection<NearbyStop> filterByCarPark(
      Collection<NearbyStop> nearbyStops, int maxCarParkAccessEgressStops
  ) {
    // Add all stops locations within range to result
    Set<NearbyStop> result = nearbyStops.stream()
        // Sorts by least distance
        .sorted()
        .limit(maxCarParkAccessEgressStops)
        .collect(Collectors.toSet());

    // Get the car park used for the stops within range
    Set<Edge> parAndRideEdges = result
        .stream()
        .filter(s -> s.edges.stream().anyMatch(e -> e instanceof ParkAndRideEdge))
        .map(s -> s.edges.stream().filter(e -> e instanceof ParkAndRideEdge).findFirst().get())
        .collect(Collectors.toSet());

    // Add all the stops that use the same car park as stops within range
    result.addAll(
        nearbyStops
            .stream()
            .filter(s -> s.edges.stream().anyMatch(parAndRideEdges::contains))
            .collect(Collectors.toList())
    );

    return result;
  }
}
