package org.opentripplanner.ext.carpooling.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.filter.FilterChain;
import org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.routing.InsertionEvaluator;
import org.opentripplanner.ext.carpooling.routing.InsertionPosition;
import org.opentripplanner.ext.carpooling.routing.InsertionPositionFinder;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link CarpoolingService} that orchestrates the two-phase
 * carpooling routing algorithm: position finding and insertion evaluation.
 * <p>
 * This service is the main entry point for carpool routing functionality. It coordinates multiple
 * components to efficiently find viable carpool matches while minimizing expensive routing
 * calculations through strategic filtering and early rejection.
 *
 * <h2>Algorithm Phases</h2>
 * <p>
 * The service executes routing requests in three distinct phases:
 * <ol>
 *   <li><strong>Pre-filtering ({@link FilterChain}):</strong> Quickly eliminates incompatible
 *       trips based on capacity, time windows, direction, and distance.</li>
 *   <li><strong>Position Finding ({@link InsertionPositionFinder}):</strong> For trips that
 *       pass filtering, identifies viable pickup/dropoff position pairs using fast heuristics
 *       (capacity, direction, beeline delay estimates). No routing is performed in this phase.</li>
 *   <li><strong>Insertion Evaluation ({@link InsertionEvaluator}):</strong> For viable positions,
 *       computes actual routes using A* street routing. Evaluates all feasible insertion positions
 *       and selects the one minimizing additional travel time while satisfying delay constraints.</li>
 * </ol>
 *
 * <h2>Component Dependencies</h2>
 * <ul>
 *   <li><strong>{@link CarpoolingRepository}:</strong> Source of available driver trips</li>
 *   <li><strong>{@link Graph}:</strong> Street network for routing calculations</li>
 *   <li><strong>{@link VertexLinker}:</strong> Links coordinates to graph vertices</li>
 *   <li><strong>{@link StreetLimitationParametersService}:</strong> Street routing configuration</li>
 *   <li><strong>{@link FilterChain}:</strong> Pre-screening filters</li>
 *   <li><strong>{@link InsertionPositionFinder}:</strong> Heuristic position filtering</li>
 *   <li><strong>{@link InsertionEvaluator}:</strong> Routing evaluation and selection</li>
 *   <li><strong>{@link CarpoolItineraryMapper}:</strong> Maps insertions to OTP itineraries</li>
 * </ul>
 *
 * @see CarpoolingService for interface documentation and usage examples
 * @see FilterChain for filtering strategy details
 * @see InsertionPositionFinder for position finding strategy details
 * @see InsertionEvaluator for insertion evaluation algorithm details
 */
public class DefaultCarpoolingService implements CarpoolingService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingService.class);
  private static final int DEFAULT_MAX_CARPOOL_RESULTS = 3;

  private final CarpoolingRepository repository;
  private final Graph graph;
  private final VertexLinker vertexLinker;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final FilterChain preFilters;
  private final CarpoolItineraryMapper itineraryMapper;

  /**
   * Creates a new carpooling service with the specified dependencies.
   * <p>
   * The service is initialized with a standard filter chain. The filter chain
   * is currently hardcoded but could be made configurable in future versions.
   *
   * @param repository provides access to active driver trips, must not be null
   * @param graph the street network used for routing calculations, must not be null
   * @param vertexLinker links coordinates to graph vertices for routing, must not be null
   * @param streetLimitationParametersService provides street routing configuration including
   *        speed limits, must not be null
   * @throws NullPointerException if any parameter is null
   */
  public DefaultCarpoolingService(
    CarpoolingRepository repository,
    Graph graph,
    VertexLinker vertexLinker,
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    this.repository = repository;
    this.graph = graph;
    this.vertexLinker = vertexLinker;
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.preFilters = FilterChain.standard();
    this.itineraryMapper = new CarpoolItineraryMapper();
  }

  @Override
  public List<Itinerary> route(RouteRequest request) throws RoutingValidationException {
    validateRequest(request);

    WgsCoordinate passengerPickup = new WgsCoordinate(request.from().getCoordinate());
    WgsCoordinate passengerDropoff = new WgsCoordinate(request.to().getCoordinate());
    var passengerDepartureTime = request.dateTime();

    LOG.debug(
      "Finding carpool itineraries from {} to {} at {}",
      passengerPickup,
      passengerDropoff,
      passengerDepartureTime
    );

    var allTrips = repository.getCarpoolTrips();
    LOG.debug("Repository contains {} carpool trips", allTrips.size());

    var candidateTrips = allTrips
      .stream()
      .filter(trip ->
        preFilters.accepts(
          trip,
          passengerPickup,
          passengerDropoff,
          passengerDepartureTime,
          request.searchWindow() == null ? Duration.ofMinutes(30) : request.searchWindow()
        )
      )
      .toList();

    LOG.debug(
      "{} trips passed pre-filters ({} rejected)",
      candidateTrips.size(),
      allTrips.size() - candidateTrips.size()
    );

    if (candidateTrips.isEmpty()) {
      return List.of();
    }

    var routingFunction = createRoutingFunction(request);

    // Phase 1: Find viable positions using fast heuristics (no routing)
    var delayConstraints = new PassengerDelayConstraints();
    var positionFinder = new InsertionPositionFinder(delayConstraints, new BeelineEstimator());

    // Phase 2: Evaluate positions with expensive A* routing
    var insertionEvaluator = new InsertionEvaluator(routingFunction, delayConstraints);

    // Find optimal insertions for remaining trips
    var insertionCandidates = candidateTrips
      .stream()
      .map(trip -> {
        List<InsertionPosition> viablePositions = positionFinder.findViablePositions(
          trip,
          passengerPickup,
          passengerDropoff
        );

        if (viablePositions.isEmpty()) {
          LOG.debug("No viable positions found for trip {} (avoided all routing!)", trip.getId());
          return null;
        }

        LOG.debug(
          "{} viable positions found for trip {}, evaluating with routing",
          viablePositions.size(),
          trip.getId()
        );

        // Evaluate only viable positions with expensive routing
        return insertionEvaluator.findBestInsertion(
          trip,
          viablePositions,
          passengerPickup,
          passengerDropoff
        );
      })
      .filter(Objects::nonNull)
      .sorted(Comparator.comparing(InsertionCandidate::additionalDuration))
      .limit(DEFAULT_MAX_CARPOOL_RESULTS)
      .toList();

    LOG.debug("Found {} viable insertion candidates", insertionCandidates.size());

    // Map to itineraries
    var itineraries = insertionCandidates
      .stream()
      .map(candidate -> itineraryMapper.toItinerary(request, candidate))
      .filter(Objects::nonNull)
      .toList();

    LOG.info("Returning {} carpool itineraries", itineraries.size());
    return itineraries;
  }

  private void validateRequest(RouteRequest request) throws RoutingValidationException {
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
  }

  /**
   * Creates a routing function that performs A* street routing between coordinate pairs.
   * <p>
   * The returned function encapsulates all dependencies needed for routing (graph, vertex linker,
   * street parameters) so that {@link InsertionEvaluator} can perform routing without
   * knowing about OTP's internal routing infrastructure. This abstraction allows the evaluator
   * to remain focused on optimization logic rather than routing mechanics.
   *
   * <h3>Routing Strategy</h3>
   * <ul>
   *   <li><strong>Mode:</strong> CAR mode for both origin and destination</li>
   *   <li><strong>Algorithm:</strong> A* with Euclidean heuristic</li>
   *   <li><strong>Vertex Linking:</strong> Creates temporary vertices at coordinate locations</li>
   *   <li><strong>Error Handling:</strong> Returns null on routing failure (logged as warning)</li>
   * </ul>
   *
   * @param request the route request containing preferences and parameters for routing
   * @return a routing function that performs A* routing between two coordinates, returning
   *         null if routing fails for any reason (network unreachable, timeout, etc.)
   */
  private InsertionEvaluator.RoutingFunction createRoutingFunction(RouteRequest request) {
    return (from, to) -> {
      try {
        var tempVertices = new TemporaryVerticesContainer(
          graph,
          vertexLinker,
          null,
          from,
          to,
          StreetMode.CAR,
          StreetMode.CAR
        );

        return carpoolRouting(
          request,
          new StreetRequest(StreetMode.CAR),
          tempVertices.getFromVertices(),
          tempVertices.getToVertices(),
          streetLimitationParametersService.getMaxCarSpeed()
        );
      } catch (Exception e) {
        LOG.warn("Routing failed from {} to {}: {}", from, to, e.getMessage());
        return null;
      }
    };
  }

  /**
   * Core A* routing for carpooling optimized for car travel.
   * <p>
   * Configures and executes an A* street search with settings optimized for carpooling:
   * <ul>
   *   <li><strong>Heuristic:</strong> Euclidean distance with max car speed for admissibility</li>
   *   <li><strong>Skip Strategy:</strong> Skips edges exceeding max direct duration limit</li>
   *   <li><strong>Dominance:</strong> Minimum weight dominance (finds shortest path)</li>
   *   <li><strong>Sorting:</strong> Results sorted by arrival time or departure time</li>
   * </ul>
   *
   * @param routeRequest the route request containing preferences and parameters
   * @param streetRequest the street request specifying CAR mode
   * @param fromVertices set of origin vertices to start routing from
   * @param toVertices set of destination vertices to route to
   * @param maxCarSpeed maximum car speed in meters/second, used for heuristic calculation
   * @return the first (best) path found, or null if no paths exist
   */
  private GraphPath<State, Edge, Vertex> carpoolRouting(
    RouteRequest routeRequest,
    StreetRequest streetRequest,
    java.util.Set<Vertex> fromVertices,
    java.util.Set<Vertex> toVertices,
    float maxCarSpeed
  ) {
    var preferences = routeRequest.preferences().street();

    var streetSearch = StreetSearchBuilder.of()
      .withHeuristic(new EuclideanRemainingWeightHeuristic(maxCarSpeed))
      .withSkipEdgeStrategy(
        new DurationSkipEdgeStrategy(preferences.maxDirectDuration().valueOf(streetRequest.mode()))
      )
      .withDominanceFunction(new DominanceFunctions.MinimumWeight())
      .withRequest(routeRequest)
      .withStreetRequest(streetRequest)
      .withFrom(fromVertices)
      .withTo(toVertices);

    List<GraphPath<State, Edge, Vertex>> paths = streetSearch.getPathsToTarget();
    paths.sort(new PathComparator(routeRequest.arriveBy()));

    if (paths.isEmpty()) {
      return null;
    }

    return paths.getFirst();
  }
}
