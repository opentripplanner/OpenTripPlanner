package org.opentripplanner.graph_builder.module.nearbystops;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.spi.SkipEdgeStrategy;
import org.opentripplanner.astar.strategy.ComposingSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.MaxCountSkipEdgeStrategy;
import org.opentripplanner.ext.dataoverlay.routing.DataOverlayContext;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.framework.application.OTPRequestTimeoutException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.StreetSearchRequestMapper;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.transit.model.site.AreaStop;

public class StreetNearbyStopFinder implements NearbyStopFinder {

  private final Duration durationLimit;
  private final int maxStopCount;
  private final DataOverlayContext dataOverlayContext;

  /**
   * Construct a NearbyStopFinder for the given graph and search radius.
   */
  public StreetNearbyStopFinder(
    Duration durationLimit,
    int maxStopCount,
    DataOverlayContext dataOverlayContext
  ) {
    this.dataOverlayContext = dataOverlayContext;
    this.durationLimit = durationLimit;
    this.maxStopCount = maxStopCount;
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. If the origin vertex is a StopVertex, the result will include it; this characteristic
   * is essential for associating the correct stop with each trip pattern in the vicinity.
   */
  @Override
  public Collection<NearbyStop> findNearbyStops(
    Vertex vertex,
    RouteRequest routingRequest,
    StreetRequest streetRequest,
    boolean reverseDirection
  ) {
    return findNearbyStops(Set.of(vertex), reverseDirection, routingRequest, streetRequest);
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. If the origin vertex is a StopVertex, the result will include it.
   *
   * @param originVertices   the origin point of the street search
   * @param reverseDirection if true the paths returned instead originate at the nearby stops and
   *                         have the originVertex as the destination
   */
  public Collection<NearbyStop> findNearbyStops(
    Set<Vertex> originVertices,
    boolean reverseDirection,
    RouteRequest request,
    StreetRequest streetRequest
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    List<NearbyStop> stopsFound = createDirectlyConnectedStops(
      originVertices,
      reverseDirection,
      request,
      streetRequest
    );

    // Return only the origin vertices if there are no valid street modes
    if (streetRequest.mode() == StreetMode.NOT_SET) {
      return stopsFound;
    }

    ShortestPathTree<State, Edge, Vertex> spt = StreetSearchBuilder
      .of()
      .setSkipEdgeStrategy(getSkipEdgeStrategy())
      .setDominanceFunction(new DominanceFunctions.MinimumWeight())
      .setRequest(request)
      .setArriveBy(reverseDirection)
      .setStreetRequest(streetRequest)
      .setFrom(reverseDirection ? null : originVertices)
      .setTo(reverseDirection ? originVertices : null)
      .setDataOverlayContext(dataOverlayContext)
      .getShortestPathTree();

    // Only used if OTPFeature.FlexRouting.isOn()
    Multimap<AreaStop, State> locationsMap = ArrayListMultimap.create();

    if (spt != null) {
      // TODO use GenericAStar and a traverseVisitor? Add an earliestArrival switch to genericAStar?
      for (State state : spt.getAllStates()) {
        Vertex targetVertex = state.getVertex();
        if (originVertices.contains(targetVertex)) continue;
        if (targetVertex instanceof TransitStopVertex tsv && state.isFinal()) {
          stopsFound.add(NearbyStop.nearbyStopForState(state, tsv.getStop()));
        }
        if (
          OTPFeature.FlexRouting.isOn() &&
          targetVertex instanceof StreetVertex streetVertex && !streetVertex.areaStops().isEmpty()
        ) {
          for (AreaStop areaStop : ((StreetVertex) targetVertex).areaStops()) {
            // This is for a simplification, so that we only return one vertex from each
            // stop location. All vertices are added to the multimap, which is filtered
            // below, so that only the closest vertex is added to stopsFound
            if (canBoardFlex(state, reverseDirection)) {
              locationsMap.put(areaStop, state);
            }
          }
        }
      }
    }

    if (OTPFeature.FlexRouting.isOn()) {
      for (var locationStates : locationsMap.asMap().entrySet()) {
        AreaStop areaStop = locationStates.getKey();
        Collection<State> states = locationStates.getValue();
        // Select the vertex from all vertices that are reachable per AreaStop by taking
        // the minimum walking distance
        State min = Collections.min(states, Comparator.comparing(State::getWeight));

        // If the best state for this AreaStop is a SplitterVertex, we want to get the
        // TemporaryStreetLocation instead. This allows us to reach SplitterVertices in both
        // directions when routing later.
        if (min.getBackState().getVertex() instanceof TemporaryStreetLocation) {
          min = min.getBackState();
        }

        stopsFound.add(NearbyStop.nearbyStopForState(min, areaStop));
      }
    }

    return stopsFound;
  }

  private SkipEdgeStrategy<State, Edge> getSkipEdgeStrategy() {
    var durationSkipEdgeStrategy = new DurationSkipEdgeStrategy(durationLimit);

    if (maxStopCount > 0) {
      var strategy = new MaxCountSkipEdgeStrategy<>(
        maxStopCount,
        StreetNearbyStopFinder::hasReachedStop
      );
      return new ComposingSkipEdgeStrategy<>(strategy, durationSkipEdgeStrategy);
    }
    return durationSkipEdgeStrategy;
  }

  private static List<NearbyStop> createDirectlyConnectedStops(
    Set<Vertex> originVertices,
    boolean reverseDirection,
    RouteRequest request,
    StreetRequest streetRequest
  ) {
    List<NearbyStop> stopsFound = new ArrayList<>();

    StreetSearchRequest streetSearchRequest = StreetSearchRequestMapper
      .mapToTransferRequest(request)
      .withArriveBy(reverseDirection)
      .withMode(streetRequest.mode())
      .build();

    /* Add the origin vertices if they are stops */
    for (Vertex vertex : originVertices) {
      if (vertex instanceof TransitStopVertex tsv) {
        stopsFound.add(
          new NearbyStop(
            tsv.getStop(),
            0,
            Collections.emptyList(),
            new State(vertex, streetSearchRequest)
          )
        );
      }
    }

    return stopsFound;
  }

  private boolean canBoardFlex(State state, boolean reverse) {
    Collection<Edge> edges = reverse
      ? state.getVertex().getIncoming()
      : state.getVertex().getOutgoing();

    return edges
      .stream()
      .anyMatch(e -> e instanceof StreetEdge se && se.getPermission().allows(TraverseMode.CAR));
  }

  /**
   * Checks if the {@code state} is at a transit vertex and if it's final, which means that the state
   * can actually board a vehicle.
   * <p>
   * This is important because there can be cases where states that cannot actually board the vehicle
   * can dominate those that can thereby leading to zero found stops when this predicate is used with
   * the {@link MaxCountSkipEdgeStrategy}.
   * <p>
   * An example of this would be an egress/reverse search with a very high walk reluctance where the
   * states that speculatively rent a vehicle move the walk states down the A* priority queue until
   * the required number of stops are reached to abort the search, leading to zero egress results.
   */
  public static boolean hasReachedStop(State state) {
    return state.getVertex() instanceof TransitStopVertex && state.isFinal();
  }
}
