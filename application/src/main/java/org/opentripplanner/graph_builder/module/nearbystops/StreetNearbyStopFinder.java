package org.opentripplanner.graph_builder.module.nearbystops;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.opentripplanner.astar.model.ShortestPathTree;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.MaxCountTerminationStrategy;
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
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.transit.model.site.AreaStop;

public class StreetNearbyStopFinder implements NearbyStopFinder {

  private final Duration durationLimit;
  private final int maxStopCount;
  private final DataOverlayContext dataOverlayContext;
  private final Set<Vertex> ignoreVertices;

  /**
   * Construct a NearbyStopFinder for the given graph and search radius.
   *
   * @param maxStopCount The maximum stops to return. 0 means no limit. Regardless of the
   *                     maxStopCount we will always return all the directly connected stops.
   */
  public StreetNearbyStopFinder(
    Duration durationLimit,
    int maxStopCount,
    DataOverlayContext dataOverlayContext
  ) {
    this(durationLimit, maxStopCount, dataOverlayContext, Set.of());
  }

  /**
   * Construct a NearbyStopFinder for the given graph and search radius.
   *
   * @param maxStopCount The maximum stops to return. 0 means no limit. Regardless of the maxStopCount
   *                     we will always return all the directly connected stops.
   * @param ignoreVertices   A set of stop vertices to ignore and not return NearbyStops for.
   */
  public StreetNearbyStopFinder(
    Duration durationLimit,
    int maxStopCount,
    DataOverlayContext dataOverlayContext,
    Set<Vertex> ignoreVertices
  ) {
    this.dataOverlayContext = dataOverlayContext;
    this.durationLimit = durationLimit;
    this.maxStopCount = maxStopCount;
    this.ignoreVertices = ignoreVertices;
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
    return findNearbyStops(Set.of(vertex), routingRequest, streetRequest, reverseDirection);
  }

  /**
   * Return all stops within a certain radius of the given vertex, using network distance along
   * streets. If the origin vertex is a StopVertex, the result will include it.
   *
   * @param originVertices   the origin point of the street search.
   * @param reverseDirection if true the paths returned instead originate at the nearby stops and
   *                         have the originVertex as the destination.
   */
  public Collection<NearbyStop> findNearbyStops(
    Set<Vertex> originVertices,
    RouteRequest request,
    StreetRequest streetRequest,
    boolean reverseDirection
  ) {
    OTPRequestTimeoutException.checkForTimeout();

    List<NearbyStop> stopsFound = NearbyStop.nearbyStopsForTransitStopVerticesFiltered(
      Sets.difference(originVertices, ignoreVertices),
      reverseDirection,
      request,
      streetRequest
    );

    // Return only the origin vertices if there are no valid street modes
    if (
      streetRequest.mode() == StreetMode.NOT_SET ||
      (maxStopCount > 0 && stopsFound.size() >= maxStopCount)
    ) {
      return stopsFound;
    }
    stopsFound = new ArrayList<>(stopsFound);

    var streetSearch = StreetSearchBuilder.of()
      .setSkipEdgeStrategy(new DurationSkipEdgeStrategy<>(durationLimit))
      .setDominanceFunction(new DominanceFunctions.MinimumWeight())
      .setRequest(request)
      .setArriveBy(reverseDirection)
      .setStreetRequest(streetRequest)
      .setFrom(reverseDirection ? null : originVertices)
      .setTo(reverseDirection ? originVertices : null)
      .setDataOverlayContext(dataOverlayContext);

    if (maxStopCount > 0) {
      streetSearch.setTerminationStrategy(
        new MaxCountTerminationStrategy<>(maxStopCount, this::hasReachedStop)
      );
    }

    ShortestPathTree<State, Edge, Vertex> spt = streetSearch.getShortestPathTree();

    // Only used if OTPFeature.FlexRouting.isOn()
    Multimap<AreaStop, State> locationsMap = ArrayListMultimap.create();

    if (spt != null) {
      // TODO use GenericAStar and a traverseVisitor? Add an earliestArrival switch to genericAStar?
      for (State state : spt.getAllStates()) {
        Vertex targetVertex = state.getVertex();
        if (originVertices.contains(targetVertex) || ignoreVertices.contains(targetVertex)) {
          continue;
        }
        if (targetVertex instanceof TransitStopVertex tsv && state.isFinal()) {
          stopsFound.add(NearbyStop.nearbyStopForState(state, tsv.getStop()));
        }
        if (
          OTPFeature.FlexRouting.isOn() &&
          targetVertex instanceof StreetVertex streetVertex &&
          !streetVertex.areaStops().isEmpty()
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
   * the {@link MaxCountTerminationStrategy}.
   * <p>
   * An example of this would be an egress/reverse search with a very high walk reluctance where the
   * states that speculatively rent a vehicle move the walk states down the A* priority queue until
   * the required number of stops are reached to abort the search, leading to zero egress results.
   */
  private boolean hasReachedStop(State state) {
    var vertex = state.getVertex();
    return (
      vertex instanceof TransitStopVertex && state.isFinal() && !ignoreVertices.contains(vertex)
    );
  }
}
