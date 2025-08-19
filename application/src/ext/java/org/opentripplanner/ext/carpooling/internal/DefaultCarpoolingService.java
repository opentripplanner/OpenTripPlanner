package org.opentripplanner.ext.carpooling.internal;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.data.KristiansandCarpoolingData;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolItineraryCandidate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.StreetPreferences;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.model.site.AreaStop;

public class DefaultCarpoolingService implements CarpoolingService {

  private static final Duration MAX_BOOKING_WINDOW = Duration.ofHours(2);
  private static final int DEFAULT_MAX_CARPOOL_RESULTS = 3;

  private final StreetLimitationParametersService streetLimitationParametersService;
  private final CarpoolingRepository repository;
  private final Graph graph;

  public DefaultCarpoolingService(StreetLimitationParametersService streetLimitationParametersService, CarpoolingRepository repository, Graph graph) {
    KristiansandCarpoolingData.populateRepository(repository, graph);
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.repository = repository;
    this.graph = graph;
  }

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
   *   MAX_COST = MAX_WALK_DISTANCE * walkReluctance + DIRECT_DISTANCE - MAX_WALK_DISTANCE
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
  public List<Itinerary> route(RouteRequest request)
    throws RoutingValidationException {
    if (
      Objects.requireNonNull(request.from()).lat == null ||
      Objects.requireNonNull(request.from()).lng == null
    ) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.FROM_PLACE))
      );
    }
    if (
      Objects.requireNonNull(request.to()).lat == null ||
      Objects.requireNonNull(request.to()).lng == null
    ) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.TO_PLACE))
      );
    }

    List<CarpoolItineraryCandidate> itineraryCandidates = getCarpoolItineraryCandidates(request);
    if (itineraryCandidates.isEmpty()) {
      // No relevant carpool trips found within the next 2 hours, return empty list
      return Collections.emptyList();
    }

    // Perform A* routing for the top candidates and create itineraries
    List<Itinerary> itineraries = new ArrayList<>();
    int maxResults = Math.min(itineraryCandidates.size(), DEFAULT_MAX_CARPOOL_RESULTS);

    for (int i = 0; i < maxResults; i++) {
      CarpoolItineraryCandidate candidate = itineraryCandidates.get(i);

      GraphPath<State, Edge, Vertex> routing = carpoolRouting(
        request,
        new StreetRequest(StreetMode.CAR),
        candidate.boardingStop().state.getVertex(),
        candidate.alightingStop().state.getVertex(),
        streetLimitationParametersService.getMaxCarSpeed());

      Itinerary itinerary = CarpoolItineraryMapper.mapToItinerary(request, candidate, routing);
      if (itinerary != null) {
        itineraries.add(itinerary);
      }
    }

    return itineraries;
  }

  private List<CarpoolItineraryCandidate> getCarpoolItineraryCandidates(RouteRequest request) {
    TemporaryVerticesContainer temporaryVertices;

    try {
      temporaryVertices = new TemporaryVerticesContainer(
        graph,
        request.from(),
        request.to(),
        request.journey().access().mode(),
        request.journey().egress().mode());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Prepare access/egress
    Collection<NearbyStop> accessStops = getClosestAreaStopsToVertex(
      request,
      request.journey().access(),
      temporaryVertices.getFromVertices(),
      null,
      repository.getBoardingAreasForVertex());

    Collection<NearbyStop> egressStops = getClosestAreaStopsToVertex(
      request,
      request.journey().egress(),
      null,
      temporaryVertices.getToVertices(),
      repository.getAlightingAreasForVertex());

    Map<CarpoolTrip, NearbyStop> tripToBoardingStop = accessStops.stream()
      .collect(Collectors.toMap(
        stop -> repository.getCarpoolTripByBoardingArea((AreaStop) stop.stop),
        stop -> stop
      ));

    Map<CarpoolTrip, NearbyStop> tripToAlightingStop = egressStops.stream()
      .collect(Collectors.toMap(
        stop -> repository.getCarpoolTripByAlightingArea((AreaStop) stop.stop),
        stop -> stop
      ));

    // Find trips that have both boarding and alighting stops
    List<CarpoolItineraryCandidate> itineraryCandidates = tripToBoardingStop.keySet().stream()
      .filter(tripToAlightingStop::containsKey)
      .map(trip -> new CarpoolItineraryCandidate(
        trip,
        tripToBoardingStop.get(trip),
        tripToAlightingStop.get(trip)
      ))
      .filter(candidate ->
        // Only include candidates that leave after first possible arrival at the boarding area
        candidate.trip().getStartTime().toInstant().isAfter(
          candidate.boardingStop().state.getTime()
        )
          // AND leave within the next 2 hours
          && candidate.trip().getStartTime().toInstant().isBefore(
            candidate.boardingStop().state.getTime().plus(MAX_BOOKING_WINDOW))
      )
      .toList();
    return itineraryCandidates;
  }

  private List<NearbyStop> getClosestAreaStopsToVertex(
    RouteRequest request,
    StreetRequest streetRequest,
    Set<Vertex> originVertices,
    Set<Vertex> destinationVertices,
    Multimap<StreetVertex, AreaStop> destinationAreas
  ) {
    var maxAccessEgressDuration = request.preferences().street().accessEgress().maxDuration().valueOf(streetRequest.mode());
    var arriveBy = originVertices == null && destinationVertices != null;

    var streetSearch = StreetSearchBuilder.of()
      .setSkipEdgeStrategy(
        new DurationSkipEdgeStrategy<>(maxAccessEgressDuration)
      )
      .setDominanceFunction(new DominanceFunctions.MinimumWeight())
      .setRequest(request)
      .setArriveBy(arriveBy)
      .setStreetRequest(streetRequest)
      .setFrom(originVertices)
      .setTo(destinationVertices);

    var spt = streetSearch.getShortestPathTree();

    if (spt == null) {
      return Collections.emptyList();
    }

    // Get the reachable AreaStops from the vertices in the SPT
    Multimap<AreaStop, State> locationsMap = ArrayListMultimap.create();
    for (State state : spt.getAllStates()) {
      Vertex targetVertex = state.getVertex();
      if (
        targetVertex instanceof StreetVertex streetVertex && destinationAreas.containsKey(streetVertex)
      ) {
        for (AreaStop areaStop : destinationAreas.get(streetVertex)) {
          locationsMap.put(areaStop, state);
        }
      }
    }

    // Map the minimum reachable state for each AreaStop and the AreaStop to NearbyStop
    List<NearbyStop> stopsFound = new ArrayList<>();
    for (var locationStates : locationsMap.asMap().entrySet()) {
      AreaStop areaStop = locationStates.getKey();
      State min = getMinState(locationStates);

      stopsFound.add(NearbyStop.nearbyStopForState(min, areaStop));
    }
    return stopsFound;
  }

  private static State getMinState(Map.Entry<AreaStop, Collection<State>> locationStates) {
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
    return min;
  }

  /**
   * Performs A* street routing between two coordinates using the specified street mode.
   * Returns the routing result with distance, time, and geometry.
   */
  @Nullable
  private GraphPath<State, Edge, Vertex> carpoolRouting(
    RouteRequest request,
    StreetRequest streetRequest,
    Vertex from,
    Vertex to,
    float maxCarSpeed
  ) {
    StreetPreferences preferences = request.preferences().street();

    var streetSearch = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic(maxCarSpeed))
      .setSkipEdgeStrategy(
        new DurationSkipEdgeStrategy(
          preferences.maxDirectDuration().valueOf(streetRequest.mode())
        )
      )
      .setDominanceFunction(new DominanceFunctions.MinimumWeight())
      .setRequest(request)
      .setStreetRequest(streetRequest)
      .setFrom(from)
      .setTo(to);

    List<GraphPath<State, Edge, Vertex>> paths = streetSearch.getPathsToTarget();
    paths.sort(new PathComparator(request.arriveBy()));

    return paths.getFirst();
  }
}
