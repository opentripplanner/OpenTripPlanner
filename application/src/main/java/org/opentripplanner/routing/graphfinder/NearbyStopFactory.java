package org.opentripplanner.routing.graphfinder;

import static org.opentripplanner.routing.graphfinder.NearbyStop.ofZeroDistance;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

public class NearbyStopFactory {

  /**
   * Create zero distance NearbyStops given a list of TransitStopVertices
   */
  public static List<NearbyStop> nearbyStopsForTransitStopVertices(
    Set<TransitStopVertex> stopVertices,
    StreetSearchRequest request
  ) {
    if (stopVertices.isEmpty()) {
      return List.of();
    }

    return stopVertices
      .stream()
      .map(s -> ofZeroDistance(s.getId(), new State(s, request)))
      .toList();
  }

  /**
   * Given a list of Vertices, find the TransitStopVertices and create zero distance NearbyStops
   * for them.
   */
  public static List<NearbyStop> nearbyStopsForTransitStopVerticesFiltered(
    Collection<? extends Vertex> vertices,
    StreetSearchRequest request
  ) {
    var transitStops = vertices
      .stream()
      .filter(v -> v instanceof TransitStopVertex)
      .map(v -> (TransitStopVertex) v)
      .collect(Collectors.toSet());

    return nearbyStopsForTransitStopVertices(transitStops, request);
  }
}
