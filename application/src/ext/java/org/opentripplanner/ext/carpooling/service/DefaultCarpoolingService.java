package org.opentripplanner.ext.carpooling.service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.astar.model.GraphPath;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints;
import org.opentripplanner.ext.carpooling.filter.AccessEgressFilterChain;
import org.opentripplanner.ext.carpooling.filter.FilterChain;
import org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.CarpoolStreetRouter;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.routing.InsertionEvaluator;
import org.opentripplanner.ext.carpooling.routing.InsertionPosition;
import org.opentripplanner.ext.carpooling.routing.InsertionPositionFinder;
import org.opentripplanner.ext.carpooling.routing.TripWithViableAccessEgress;
import org.opentripplanner.ext.carpooling.routing.ViableAccessEgress;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.ext.carpooling.util.StreetVertexUtils;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.graph_builder.module.nearbystops.StopResolver;
import org.opentripplanner.graph_builder.module.nearbystops.StreetNearbyStopFinder;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.TimeUtils;
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
  static final int DEFAULT_MAX_CARPOOL_DIRECT_RESULTS = 3;
  private static final Duration DEFAULT_SEARCH_WINDOW = Duration.ofMinutes(30);
  // How far away in time a carpooling trip can be from the requested departure time to be considered
  private static final Duration ACCESS_EGRESS_SEARCH_WINDOW = Duration.ofHours(12);
  /*
    The time it takes to pick up or drop off a passenger and start driving again.
    It is only used for access/egress, and is a temporary solution.
    The implementation will be changed for both direct and access/egress when implementing the field
    latestExpectedArrivalTime from siri.
   */
  static final Duration CARPOOL_STOP_DURATION = Duration.ofMinutes(1);
  /*
    This is needed for managing computational complexity unless we find a smarter way of searching
    for nearby stops.
   */
  public static final Duration MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS =
    Duration.ofMinutes(60);
  private final CarpoolingRepository repository;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final FilterChain preFilters;
  private final AccessEgressFilterChain accessEgressPreFilters;
  private final CarpoolItineraryMapper itineraryMapper;
  private final PassengerDelayConstraints delayConstraints;
  private final InsertionPositionFinder positionFinder;
  private final VertexLinker vertexLinker;

  /**
   * Creates a new carpooling service with the specified dependencies.
   * <p>
   * The service is initialized with a standard filter chain. The filter chain
   * is currently hardcoded but could be made configurable in future versions.
   *
   * @param repository provides access to active driver trips, must not be null
   * @param streetLimitationParametersService provides street routing configuration including
   *        speed limits, must not be null
   * @param transitService provides timezone from GTFS agency data for time conversions, must not be null
   * @param vertexLinker links coordinates to graph vertices, must not be null
   * @throws NullPointerException if any parameter is null
   */
  public DefaultCarpoolingService(
    CarpoolingRepository repository,
    StreetLimitationParametersService streetLimitationParametersService,
    TransitService transitService,
    VertexLinker vertexLinker
  ) {
    this.repository = repository;
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.preFilters = FilterChain.standard();
    this.accessEgressPreFilters = AccessEgressFilterChain.standard();
    this.itineraryMapper = new CarpoolItineraryMapper(transitService.getTimeZone());
    this.delayConstraints = new PassengerDelayConstraints();
    this.positionFinder = new InsertionPositionFinder(delayConstraints, new BeelineEstimator());
    this.vertexLinker = vertexLinker;
  }

  /**
   * Routes a direct carpool trip from the passenger's origin to destination.
   * <p>
   * This method executes the full three-phase carpooling algorithm:
   * <ol>
   *   <li><strong>Pre-filtering:</strong> All trips from the repository are filtered by capacity,
   *       time window, direction, and distance to quickly eliminate incompatible matches.</li>
   *   <li><strong>Position finding:</strong> For each surviving trip, viable pickup/dropoff
   *       insertion positions are identified using beeline heuristics (no routing).</li>
   *   <li><strong>Insertion evaluation:</strong> Viable positions are evaluated with A* street
   *       routing to find the insertion that minimizes additional driver travel time while
   *       respecting delay constraints.</li>
   * </ol>
   * <p>
   * Results are sorted by additional travel time and limited to
   * {@value #DEFAULT_MAX_CARPOOL_DIRECT_RESULTS} itineraries.
   *
   * @param request the routing request. Must have {@link StreetMode#CARPOOL} as the direct mode.
   * @param linkingContext pre-linked vertices for the passenger's origin and destination
   * @return a list of carpool itineraries sorted by additional travel time, or an empty list
   *         if no viable matches are found or the direct mode is not CARPOOL
   * @throws RoutingValidationException if origin or destination coordinates are missing
   */
  @Override
  public List<Itinerary> routeDirect(RouteRequest request, LinkingContext linkingContext)
    throws RoutingValidationException {
    if (!StreetMode.CARPOOL.equals(request.journey().direct().mode())) {
      return Collections.emptyList();
    }

    validateRequest(request);

    WgsCoordinate passengerPickup = new WgsCoordinate(request.from().getCoordinate());
    WgsCoordinate passengerDropoff = new WgsCoordinate(request.to().getCoordinate());
    var passengerDepartureTime = request.dateTime();
    var searchWindow = request.searchWindow() == null
      ? DEFAULT_SEARCH_WINDOW
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

    var itineraries = List.<Itinerary>of();
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var router = new CarpoolStreetRouter(streetLimitationParametersService, request);

      var streetVertexUtils = new StreetVertexUtils(this.vertexLinker, temporaryVerticesContainer);

      var insertionEvaluator = new InsertionEvaluator(
        delayConstraints,
        linkingContext,
        streetVertexUtils,
        router
      );

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

          var tripWithVertices = CarpoolTripWithVertices.create(
            trip,
            streetVertexUtils,
            linkingContext
          );

          if (tripWithVertices == null) {
            LOG.error("Could not resolve vertices for trip {}", trip.getId());
            return null;
          }

          // Evaluate only viable positions with expensive routing
          return insertionEvaluator.findBestInsertion(
            tripWithVertices,
            viablePositions,
            passengerPickup,
            passengerDropoff
          );
        })
        .filter(Objects::nonNull)
        .sorted(Comparator.comparing(InsertionCandidate::additionalDuration))
        .limit(DEFAULT_MAX_CARPOOL_DIRECT_RESULTS)
        .toList();

      LOG.debug("Found {} viable insertion candidates", insertionCandidates.size());

      itineraries = insertionCandidates
        .stream()
        .map(candidate -> itineraryMapper.toItinerary(request, candidate))
        .filter(Objects::nonNull)
        .toList();
    }

    LOG.info("Returning {} carpool itineraries", itineraries.size());
    return itineraries;
  }

  /**
   * Routes carpool access or egress legs connecting the passenger to/from transit stops.
   * <p>
   * For <strong>access</strong>, this finds carpool rides from the passenger's origin to nearby
   * transit stops. For <strong>egress</strong>, it finds rides from nearby transit stops to the
   * passenger's destination.
   * <p>
   * The method proceeds as follows:
   * <ol>
   *   <li>Pre-filters trips using time and distance heuristic.</li>
   *   <li>Finds nearby transit stops reachable by car from the passenger's location using
   *       {@link StreetNearbyStopFinder}.</li>
   *   <li>For each candidate trip and nearby stop combination, identifies viable insertion
   *       positions using beeline heuristics.</li>
   *   <li>Evaluates viable positions with A* routing via {@link CarpoolTreeStreetRouter}.</li>
   *   <li>Converts the best insertions into {@link CarpoolAccessEgress} objects with timing
   *       information relative to {@code transitSearchTimeZero} for Raptor integration.</li>
   * </ol>
   *
   * @param request the routing request
   * @param streetRequest
   * @param accessOrEgress whether this is an access leg (origin to transit) or egress leg
   *        (transit to destination)
   * @param stopResolver used for nearby stop search
   * @param linkingContext pre-linked vertices for the passenger's origin and destination
   * @param transitSearchTimeZero the reference time for computing relative start/end times
   *        used by Raptor
   * @return a list of {@link CarpoolAccessEgress} results for Raptor, or an empty list if the
   *         request mode is not CARPOOL or no viable matches are found
   * @throws RoutingValidationException if origin or destination coordinates are missing
   */
  @Override
  public List<CarpoolAccessEgress> routeAccessEgress(
    RouteRequest request,
    StreetRequest streetRequest,
    AccessEgressType accessOrEgress,
    StopResolver stopResolver,
    LinkingContext linkingContext,
    ZonedDateTime transitSearchTimeZero
  ) throws RoutingValidationException {
    if (
      !StreetMode.CARPOOL.equals(request.journey().access().mode()) && accessOrEgress.isAccess()
    ) {
      return Collections.emptyList();
    }

    if (
      !StreetMode.CARPOOL.equals(request.journey().egress().mode()) && accessOrEgress.isEgress()
    ) {
      return Collections.emptyList();
    }

    validateRequest(request);

    var allTrips = repository.getCarpoolTrips();
    LOG.debug("Repository contains {} carpool trips", allTrips.size());

    /*
      The passenger's origin if the request is for access,
      or the passenger's destination if the request is for egress
     */
    GenericLocation passengerLocation = accessOrEgress.isAccess() ? request.from() : request.to();
    WgsCoordinate passengerCoordinates = new WgsCoordinate(
      passengerLocation.lat,
      passengerLocation.lng
    );

    var passengerDepartureTime = request.dateTime();

    var candidateTrips = allTrips
      .stream()
      .filter(trip ->
        accessEgressPreFilters.accepts(
          trip,
          passengerCoordinates,
          passengerDepartureTime,
          ACCESS_EGRESS_SEARCH_WINDOW
        )
      )
      .toList();

    if (candidateTrips.isEmpty()) {
      return List.of();
    }

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetVertexUtils = new StreetVertexUtils(this.vertexLinker, temporaryVerticesContainer);

      var carpoolTreeVertexRouter = new CarpoolTreeStreetRouter();
      Vertex passengerAccessEgressVertex = streetVertexUtils.getOrCreateVertex(
        passengerCoordinates,
        linkingContext
      );

      if (passengerAccessEgressVertex == null) {
        LOG.error("Could not link passenger coordinates {} to graph", passengerCoordinates);
        return List.of();
      }

      var streetNearbyStopFinder = StreetNearbyStopFinder.of(
        stopResolver,
        MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS,
        0
      );

      var nearbyStops = streetNearbyStopFinder
        .build()
        .findNearbyStops(
          Set.of(passengerAccessEgressVertex),
          request,
          StreetMode.CAR,
          accessOrEgress.isEgress()
        )
        .stream()
        .filter(stop -> !(stop.stop instanceof AreaStop))
        .toList();

      var nearbyStopsWithVertices = new HashMap<NearbyStop, Vertex>();
      for (var stop : nearbyStops) {
        var vertex = streetVertexUtils.getOrCreateVertex(stop.stop.getCoordinate(), linkingContext);
        if (vertex != null) {
          nearbyStopsWithVertices.put(stop, vertex);
        }
      }

      var candidateTripsWithVertices = candidateTrips
        .stream()
        .map(carpoolTrip ->
          CarpoolTripWithVertices.create(carpoolTrip, streetVertexUtils, linkingContext)
        )
        .filter(Objects::nonNull)
        .toList();

      // vertices have to be added to the carpoolTreeVertexRouter AFTER all vertices have been created
      carpoolTreeVertexRouter.addVertex(
        passengerAccessEgressVertex,
        CarpoolTreeStreetRouter.Direction.BOTH,
        MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS
      );
      candidateTripsWithVertices.forEach(tripWithVertices -> {
        var vertices = tripWithVertices.vertices();
        carpoolTreeVertexRouter.addVertex(
          vertices.getFirst(),
          CarpoolTreeStreetRouter.Direction.FROM,
          MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS
        );
        carpoolTreeVertexRouter.addVertex(
          vertices.getLast(),
          CarpoolTreeStreetRouter.Direction.TO,
          MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS
        );

        var middleVertices = vertices.subList(1, vertices.size() - 1);
        middleVertices.forEach(vertex -> {
          carpoolTreeVertexRouter.addVertex(
            vertex,
            CarpoolTreeStreetRouter.Direction.BOTH,
            MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS
          );
        });
      });

      var insertionEvaluator = new InsertionEvaluator(
        delayConstraints,
        linkingContext,
        streetVertexUtils,
        carpoolTreeVertexRouter
      );

      var candidateTripsWithViableStopsAndPositions = candidateTripsWithVertices
        .stream()
        .map(tripWithVertices -> {
          var viableSegmentInsertions = nearbyStopsWithVertices
            .keySet()
            .stream()
            .map(nearbyStop -> {
              var pickUpCoord = accessOrEgress.isAccess()
                ? passengerCoordinates
                : nearbyStop.stop.getCoordinate();
              var dropOffCoord = accessOrEgress.isAccess()
                ? nearbyStop.stop.getCoordinate()
                : passengerCoordinates;

              var viablePositions = positionFinder.findViablePositions(
                tripWithVertices.trip(),
                pickUpCoord,
                dropOffCoord
              );
              return new ViableAccessEgress(
                nearbyStop,
                nearbyStopsWithVertices.get(nearbyStop),
                passengerAccessEgressVertex,
                accessOrEgress,
                viablePositions
              );
            })
            .filter(it -> !it.insertionPositions().isEmpty())
            .toList();
          return new TripWithViableAccessEgress(tripWithVertices, viableSegmentInsertions);
        })
        .toList();

      var insertionCandidates = candidateTripsWithViableStopsAndPositions
        .stream()
        .flatMap(it -> insertionEvaluator.findBestInsertions(it).stream())
        .toList();

      return insertionCandidates
        .stream()
        .map(it ->
          createCarpoolAccessEgress(
            it,
            transitSearchTimeZero,
            /*
              Using the reluctance of mode car.
              TODO: Figure out whether carpooling should have its own reluctance variable
             */
            request.preferences().car().reluctance()
          )
        )
        .toList();
    }
  }

  private void validateRequest(RouteRequest request) throws RoutingValidationException {
    Objects.requireNonNull(request.from());
    Objects.requireNonNull(request.to());
    if (request.from().lat == null || request.from().lng == null) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.FROM_PLACE))
      );
    }
    if (request.to().lat == null || request.to().lng == null) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.TO_PLACE))
      );
    }
  }

  private Duration getTotalDurationOfSegments(
    List<GraphPath<State, Edge, Vertex>> segments,
    Duration extraTimeForStop
  ) {
    return segments
      .stream()
      .map(it -> Duration.between(it.states.getFirst().getTime(), it.states.getLast().getTime()))
      .reduce(Duration.ZERO, Duration::plus)
      .plus(extraTimeForStop.multipliedBy(segments.size() - 1));
  }

  private CarpoolAccessEgress createCarpoolAccessEgress(
    InsertionCandidate insertionCandidate,
    ZonedDateTime transitSearchTimeZero,
    Double carpoolReluctance
  ) {
    var pickUpIndex = insertionCandidate.pickupPosition();
    var dropOffIndex = insertionCandidate.dropoffPosition() - 1;

    var segmentsBeforeInsertion = insertionCandidate.routeSegments().subList(0, pickUpIndex);
    var segmentsWithPassenger = insertionCandidate
      .routeSegments()
      .subList(pickUpIndex, dropOffIndex + 1);

    var durationBeforeInsertion = getTotalDurationOfSegments(
      segmentsBeforeInsertion,
      CARPOOL_STOP_DURATION
    );

    // Adding an extra CARPOOL_STOP_DURATION for the time it takes to pick up the passenger
    var durationWithPassenger = getTotalDurationOfSegments(
      segmentsWithPassenger,
      CARPOOL_STOP_DURATION
    ).plus(CARPOOL_STOP_DURATION);

    var startTimeOfSegment = insertionCandidate.trip().startTime().plus(durationBeforeInsertion);
    var endTimeOfSegment = startTimeOfSegment.plus(durationWithPassenger);

    var relativeStartTime = TimeUtils.toTransitTimeSeconds(
      transitSearchTimeZero,
      startTimeOfSegment.toInstant()
    );
    var relativeEndTime = TimeUtils.toTransitTimeSeconds(
      transitSearchTimeZero,
      endTimeOfSegment.toInstant()
    );

    var accessEgress = new CarpoolAccessEgress(
      insertionCandidate.transitStop().stop.getIndex(),
      durationWithPassenger,
      relativeStartTime,
      relativeEndTime,
      segmentsWithPassenger,
      TimeAndCost.ZERO,
      carpoolReluctance
    );

    return accessEgress;
  }
}
