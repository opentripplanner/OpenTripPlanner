package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.model.GenericLocation;
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
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.StreetSearchBuilder;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.strategy.DominanceFunctions;
import org.opentripplanner.street.search.strategy.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.street.service.StreetLimitationParametersService;

public class DefaultCarpoolingService implements CarpoolingService {

  private static final Duration MAX_BOOKING_WINDOW = Duration.ofHours(2);
  private static final int DEFAULT_MAX_CARPOOL_RESULTS = 3;

  private final StreetLimitationParametersService streetLimitationParametersService;
  private final CarpoolingRepository repository;
  private final Graph graph;
  private final VertexLinker vertexLinker;

  public DefaultCarpoolingService(
    StreetLimitationParametersService streetLimitationParametersService,
    CarpoolingRepository repository,
    Graph graph,
    VertexLinker vertexLinker
  ) {
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.repository = repository;
    this.graph = graph;
    this.vertexLinker = vertexLinker;
  }

  /**
   * VIA-SEARCH ALGORITHM FOR CARPOOLING INTEGRATION
   *
   * <pre>
   * The core challenge of carpooling integration is matching passengers with drivers
   * based on route compatibility. This is fundamentally a via-point routing problem
   * where we need to determine if a driver's journey from A→B can accommodate a
   * passenger's journey from C→D within the driver's stated deviation tolerance.
   *
   * Algorithm Overview:
   * 1. Driver has a baseline route from origin A to destination B
   * 2. Driver specifies a deviation tolerance (deviationBudget in CarpoolTrip)
   * 3. Passenger requests travel from origin C to destination D
   * 4. Algorithm checks if route A→C→D→B stays within constraint:
   *    total_time ≤ baseline_time + deviation_tolerance
   *
   * Multi-Stage Processing:
   * Stage 1: Get all available carpool trips from repository
   * Stage 2: For each trip, calculate baseline route time (A→B)
   * Stage 3: Calculate via-route segments:
   *   - A→C: Driver's detour to pickup point
   *   - C→D: Shared journey segment
   *   - D→B: Driver's continuation to final destination
   * Stage 4: Feasibility check - compare total time vs baseline + deviationBudget
   * Stage 5: Return viable matches ranked by efficiency
   * </pre>
   */
  public List<Itinerary> route(RouteRequest request) throws RoutingValidationException {
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

    // Get all available carpool trips from repository
    List<CarpoolTrip> availableTrips = repository
      .getCarpoolTrips()
      .stream()
      .filter(trip -> {
        // Only include trips that start within the next 2 hours
        var tripStartTime = trip.startTime().toInstant();
        var requestTime = request.dateTime();
        return (
          // Currently we only include trips that start after the request time
          // We should also consider trips that start before request time and make it to the
          // pickup location on time.
          tripStartTime.isAfter(requestTime) &&
          tripStartTime.isBefore(requestTime.plus(MAX_BOOKING_WINDOW))
        );
      })
      .toList();

    if (availableTrips.isEmpty()) {
      return Collections.emptyList();
    }

    // Evaluate each carpool trip using via-search algorithm
    List<ViaCarpoolCandidate> viableCandidates = new ArrayList<>();

    for (CarpoolTrip trip : availableTrips) {
      ViaCarpoolCandidate candidate = evaluateViaRouteForTrip(request, trip);
      if (candidate != null) {
        viableCandidates.add(candidate);
      }
    }

    // Sort candidates by efficiency (additional time for driver)
    viableCandidates.sort(Comparator.comparing(ViaCarpoolCandidate::viaDeviation));

    // Create itineraries for top results
    List<Itinerary> itineraries = new ArrayList<>();
    int maxResults = Math.min(viableCandidates.size(), DEFAULT_MAX_CARPOOL_RESULTS);

    for (int i = 0; i < maxResults; i++) {
      ViaCarpoolCandidate candidate = viableCandidates.get(i);
      Itinerary itinerary = CarpoolItineraryMapper.mapViaRouteToItinerary(request, candidate);
      if (itinerary != null) {
        itineraries.add(itinerary);
      }
    }

    return itineraries;
  }

  /**
   * Evaluate a single carpool trip using the via-search algorithm.
   * Returns a viable candidate if the route A→C→D→B stays within the deviationBudget.
   */
  private ViaCarpoolCandidate evaluateViaRouteForTrip(RouteRequest request, CarpoolTrip trip) {
    TemporaryVerticesContainer acTempVertices;
    try {
      acTempVertices = new TemporaryVerticesContainer(
        graph,
        vertexLinker,
        null,
        GenericLocation.fromCoordinate(trip.boardingArea().getLat(), trip.boardingArea().getLon()),
        GenericLocation.fromCoordinate(request.from().lat, request.from().lng),
        StreetMode.CAR, // We'll route by car for all segments
        StreetMode.CAR
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    TemporaryVerticesContainer cdTempVertices;
    try {
      cdTempVertices = new TemporaryVerticesContainer(
        graph,
        vertexLinker,
        null,
        GenericLocation.fromCoordinate(request.from().lat, request.from().lng),
        GenericLocation.fromCoordinate(request.to().lat, request.to().lng),
        StreetMode.CAR, // We'll route by car for all segments
        StreetMode.CAR
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    TemporaryVerticesContainer dbTempVertices;
    try {
      dbTempVertices = new TemporaryVerticesContainer(
        graph,
        vertexLinker,
        null,
        GenericLocation.fromCoordinate(request.to().lat, request.to().lng),
        GenericLocation.fromCoordinate(
          trip.alightingArea().getLat(),
          trip.alightingArea().getLon()
        ),
        StreetMode.CAR, // We'll route by car for all segments
        StreetMode.CAR
      );
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // Calculate via-route segments: A→C→D→B
    GraphPath<State, Edge, Vertex> pickupRoute = performCarRouting(
      request,
      acTempVertices.getFromVertices(),
      acTempVertices.getToVertices()
    );
    GraphPath<State, Edge, Vertex> sharedRoute = performCarRouting(
      request,
      cdTempVertices.getFromVertices(),
      cdTempVertices.getToVertices()
    );
    GraphPath<State, Edge, Vertex> dropoffRoute = performCarRouting(
      request,
      dbTempVertices.getFromVertices(),
      dbTempVertices.getToVertices()
    );

    if (pickupRoute == null || sharedRoute == null || dropoffRoute == null) {
      return null; // Failed to calculate some segments
    }

    // Calculate total travel times
    var viaDuration = routeDuration(pickupRoute)
      .plus(routeDuration(sharedRoute))
      .plus(routeDuration(dropoffRoute));

    // Check if within deviation budget
    var deviationDuration = viaDuration.minus(trip.tripDuration());
    if (deviationDuration.compareTo(trip.deviationBudget()) > 0) {
      return null; // Exceeds deviation budget
    }

    return new ViaCarpoolCandidate(
      trip,
      pickupRoute,
      sharedRoute,
      deviationDuration,
      viaDuration,
      cdTempVertices.getFromVertices(),
      cdTempVertices.getToVertices()
    );
  }

  /**
   * Performs car routing between two vertices.
   */
  private GraphPath<State, Edge, Vertex> performCarRouting(
    RouteRequest request,
    Set<Vertex> from,
    Set<Vertex> to
  ) {
    return carpoolRouting(
      request,
      new StreetRequest(StreetMode.CAR),
      from,
      to,
      streetLimitationParametersService.getMaxCarSpeed()
    );
  }

  /**
   * Calculate the travel time in seconds for a given route.
   */
  private Duration routeDuration(GraphPath<State, Edge, Vertex> route) {
    if (route == null || route.states.isEmpty()) {
      return Duration.ZERO;
    }
    return Duration.between(route.states.getFirst().getTime(), route.states.getLast().getTime());
  }

  /**
   * Performs A* street routing between two coordinates using the specified street mode.
   * Returns the routing result with distance, time, and geometry.
   */
  @Nullable
  private GraphPath<State, Edge, Vertex> carpoolRouting(
    RouteRequest request,
    StreetRequest streetRequest,
    Set<Vertex> from,
    Set<Vertex> to,
    float maxCarSpeed
  ) {
    StreetPreferences preferences = request.preferences().street();

    var streetSearch = StreetSearchBuilder.of()
      .setHeuristic(new EuclideanRemainingWeightHeuristic(maxCarSpeed))
      .setSkipEdgeStrategy(
        new DurationSkipEdgeStrategy(preferences.maxDirectDuration().valueOf(streetRequest.mode()))
      )
      .setDominanceFunction(new DominanceFunctions.MinimumWeight())
      .setRequest(request)
      .setStreetRequest(streetRequest)
      .setFrom(from)
      .setTo(to);

    List<GraphPath<State, Edge, Vertex>> paths = streetSearch.getPathsToTarget();
    paths.sort(new PathComparator(request.arriveBy()));

    if (paths.isEmpty()) {
      return null;
    }

    return paths.getFirst();
  }
}
