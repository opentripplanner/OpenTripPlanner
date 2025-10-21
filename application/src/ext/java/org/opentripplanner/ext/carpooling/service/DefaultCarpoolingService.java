package org.opentripplanner.ext.carpooling.service;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.filter.FilterChain;
import org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.routing.CarpoolStreetRouter;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.routing.InsertionEvaluator;
import org.opentripplanner.ext.carpooling.routing.InsertionPosition;
import org.opentripplanner.ext.carpooling.routing.InsertionPositionFinder;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.linking.VertexLinker;
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
  private final PassengerDelayConstraints delayConstraints;
  private final InsertionPositionFinder positionFinder;

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
    this.delayConstraints = new PassengerDelayConstraints();
    this.positionFinder = new InsertionPositionFinder(delayConstraints, new BeelineEstimator());
  }

  @Override
  public List<Itinerary> route(RouteRequest request) throws RoutingValidationException {
    validateRequest(request);

    WgsCoordinate passengerPickup = new WgsCoordinate(request.from().getCoordinate());
    WgsCoordinate passengerDropoff = new WgsCoordinate(request.to().getCoordinate());
    var passengerDepartureTime = request.dateTime();
    var searchWindow = request.searchWindow() == null
      ? Duration.ofMinutes(30)
      : request.searchWindow();

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
          searchWindow
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

    var router = new CarpoolStreetRouter(
      graph,
      vertexLinker,
      streetLimitationParametersService,
      request
    );
    var insertionEvaluator = new InsertionEvaluator(router::route, delayConstraints);

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
}
