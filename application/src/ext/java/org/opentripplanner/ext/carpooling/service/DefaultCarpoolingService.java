package org.opentripplanner.ext.carpooling.service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.astar.strategy.DurationSkipEdgeStrategy;
import org.opentripplanner.astar.strategy.PathComparator;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.filter.FilterChain;
import org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.routing.OptimalInsertionStrategy;
import org.opentripplanner.ext.carpooling.validation.CompositeValidator;
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
 * Refactored carpooling service using the new modular architecture.
 * <p>
 * Orchestrates:
 * - Pre-filtering trips with FilterChain
 * - Finding optimal insertions with OptimalInsertionStrategy
 * - Mapping results to itineraries with CarpoolItineraryMapper
 */
public class DefaultCarpoolingService implements CarpoolingService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingService.class);
  private static final int DEFAULT_MAX_CARPOOL_RESULTS = 3;

  private final CarpoolingRepository repository;
  private final Graph graph;
  private final VertexLinker vertexLinker;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final FilterChain preFilters;
  private final CompositeValidator insertionValidator;
  private final CarpoolItineraryMapper itineraryMapper;

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
    this.insertionValidator = CompositeValidator.standard();
    this.itineraryMapper = new CarpoolItineraryMapper();
  }

  @Override
  public List<Itinerary> route(RouteRequest request) throws RoutingValidationException {
    // Validate request
    validateRequest(request);

    // Extract passenger coordinates and time
    WgsCoordinate passengerPickup = new WgsCoordinate(request.from().getCoordinate());
    WgsCoordinate passengerDropoff = new WgsCoordinate(request.to().getCoordinate());
    var passengerDepartureTime = request.dateTime();

    LOG.debug(
      "Finding carpool itineraries from {} to {} at {}",
      passengerPickup,
      passengerDropoff,
      passengerDepartureTime
    );

    // Get all trips from repository
    var allTrips = repository.getCarpoolTrips();
    LOG.debug("Repository contains {} carpool trips", allTrips.size());

    // Apply pre-filters (fast rejection) - pass time for time-aware filters
    var candidateTrips = allTrips
      .stream()
      .filter(trip ->
        preFilters.accepts(trip, passengerPickup, passengerDropoff, passengerDepartureTime)
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

    // Create routing function
    var routingFunction = createRoutingFunction(request);

    // Create insertion strategy
    var insertionStrategy = new OptimalInsertionStrategy(insertionValidator, routingFunction);

    // Find optimal insertions for remaining trips
    var insertionCandidates = candidateTrips
      .stream()
      .map(trip -> insertionStrategy.findOptimalInsertion(trip, passengerPickup, passengerDropoff))
      .filter(Objects::nonNull)
      .filter(InsertionCandidate::isWithinDeviationBudget)
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

  /**
   * Validates the route request.
   */
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
   * Creates a routing function that performs A* street routing.
   */
  private OptimalInsertionStrategy.RoutingFunction createRoutingFunction(RouteRequest request) {
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

        return performCarRouting(
          request,
          tempVertices.getFromVertices(),
          tempVertices.getToVertices()
        );
      } catch (Exception e) {
        LOG.warn("Routing failed from {} to {}: {}", from, to, e.getMessage());
        return null;
      }
    };
  }

  /**
   * Performs A* car routing between two vertex sets.
   */
  private GraphPath<State, Edge, Vertex> performCarRouting(
    RouteRequest request,
    java.util.Set<Vertex> from,
    java.util.Set<Vertex> to
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
   * Core A* routing for carpooling (optimized for car travel).
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
