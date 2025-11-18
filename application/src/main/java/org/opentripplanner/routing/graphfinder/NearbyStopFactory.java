package org.opentripplanner.routing.graphfinder;

import static org.opentripplanner.routing.graphfinder.NearbyStop.ofZeroDistance;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transit.model.site.RegularStop;

public class NearbyStopFactory {

  private final StopResolver stopResolver;

  public NearbyStopFactory(StopResolver stopResolver) {
    this.stopResolver = Objects.requireNonNull(stopResolver);
  }

  /**
   * Create zero distance NearbyStops given a list of TransitStopVertices
   */
  public List<NearbyStop> nearbyStopsForTransitStopVertices(
    Set<TransitStopVertex> stopVertices,
    boolean reverseDirection,
    RouteRequest routeRequest,
    StreetRequest streetRequest
  ) {
    if (stopVertices.isEmpty()) {
      return List.of();
    }

    var streetSearchRequest = StreetSearchRequestMapper.mapToTransferRequest(routeRequest)
      .withArriveBy(reverseDirection)
      .withMode(streetRequest.mode())
      .build();

    return stopVertices
      .stream()
      .map(s -> ofZeroDistance(stop(s.getId()), new State(s, streetSearchRequest)))
      .toList();
  }

  /**
   * Given a list of Vertices, find the TransitStopVertices and create zero distance NearbyStops
   * for them.
   */
  public List<NearbyStop> nearbyStopsForTransitStopVerticesFiltered(
    Collection<? extends Vertex> vertices,
    boolean reverseDirection,
    RouteRequest routeRequest,
    StreetRequest streetRequest
  ) {
    var transitStops = vertices
      .stream()
      .filter(v -> v instanceof TransitStopVertex)
      .map(v -> (TransitStopVertex) v)
      .collect(Collectors.toSet());

    return nearbyStopsForTransitStopVertices(
      transitStops,
      reverseDirection,
      routeRequest,
      streetRequest
    );
  }

  private RegularStop stop(FeedScopedId id) {
    return Objects.requireNonNull(stopResolver.getStop(id));
  }
}
