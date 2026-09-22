package org.opentripplanner.ext.carpooling.service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.filter.CarpoolingRequest;
import org.opentripplanner.ext.carpooling.filter.ItineraryPostFilters;
import org.opentripplanner.ext.carpooling.filter.TripPreFilters;
import org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor;
import org.opentripplanner.ext.carpooling.routing.CarpoolStopIndex;
import org.opentripplanner.ext.carpooling.routing.CarpoolStreetRouter;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter;
import org.opentripplanner.ext.carpooling.routing.CorridorRouter;
import org.opentripplanner.ext.carpooling.routing.EndpointLabel;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.routing.InsertionEvaluator;
import org.opentripplanner.ext.carpooling.routing.InsertionPosition;
import org.opentripplanner.ext.carpooling.routing.InsertionPositionFinder;
import org.opentripplanner.ext.carpooling.routing.PassengerSnap;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.TripWithViableAccessEgress;
import org.opentripplanner.ext.carpooling.routing.ViableAccessEgress;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.ext.carpooling.util.StreetVertexUtils;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitServiceResolver;
import org.opentripplanner.utils.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link CarpoolingService} that orchestrates the carpooling routing
 * algorithm: pre-filtering, position finding, insertion evaluation, and post-filtering.
 * <p>
 * This service is the main entry point for carpool routing functionality. It coordinates multiple
 * components to efficiently find viable carpool matches while minimizing expensive routing
 * calculations through strategic filtering and early rejection.
 *
 * <h2>Algorithm Phases</h2>
 * <p>
 * The service executes routing requests in four phases:
 * <ol>
 *   <li><strong>Pre-filtering ({@link TripPreFilters}):</strong> Quickly eliminates incompatible
 *       trips based on capacity, time windows, and distance.</li>
 *   <li><strong>Position Finding ({@link InsertionPositionFinder}):</strong> For trips that
 *       pass filtering, identifies viable pickup/dropoff position pairs using fast heuristics
 *       (capacity, beeline delay estimates). No routing is performed in this phase.</li>
 *   <li><strong>Insertion Evaluation ({@link InsertionEvaluator}):</strong> For viable positions,
 *       computes actual routes using A* street routing. Evaluates all feasible insertion positions
 *       and selects the one minimizing additional travel time while satisfying delay constraints.</li>
 *   <li><strong>Post-filtering ({@link ItineraryPostFilters}, direct routing only):</strong>
 *       Re-checks the fully-routed {@link Itinerary} against tight time bounds that the loose
 *       pre-filter could not enforce. Access/egress routing skips this phase because it emits
 *       {@link CarpoolAccessEgress} objects rather than itineraries.</li>
 * </ol>
 *
 * <h2>Component Dependencies</h2>
 * <ul>
 *   <li><strong>{@link CarpoolingRepository}:</strong> Source of available driver trips</li>
 *   <li><strong>{@link VertexCreationService}:</strong> Links coordinates to graph vertices</li>
 *   <li><strong>{@link StreetLimitationParametersService}:</strong> Street routing configuration</li>
 *   <li><strong>{@link TripPreFilters}:</strong> Pre-screening filters</li>
 *   <li><strong>{@link InsertionPositionFinder}:</strong> Heuristic position filtering</li>
 *   <li><strong>{@link InsertionEvaluator}:</strong> Routing evaluation and selection</li>
 *   <li><strong>{@link CarpoolItineraryMapper}:</strong> Maps insertions to OTP itineraries</li>
 *   <li><strong>{@link ItineraryPostFilters}:</strong> Tight time-window enforcement on routed
 *       itineraries (direct routing only)</li>
 * </ul>
 *
 * @see CarpoolingService for interface documentation and usage examples
 * @see TripPreFilters for filtering strategy details
 * @see InsertionPositionFinder for position finding strategy details
 * @see InsertionEvaluator for insertion evaluation algorithm details
 * @see ItineraryPostFilters for post-filter behaviour
 */
public class DefaultCarpoolingService implements CarpoolingService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingService.class);

  private final CarpoolingRepository repository;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final TripPreFilters preFilters;
  private final ItineraryPostFilters postFilters;
  private final CarpoolItineraryMapper itineraryMapper;
  private final InsertionPositionFinder positionFinder;
  private final VertexCreationService vertexCreationService;

  /**
   * Snaps passenger origin/destination and transit stops onto vertices a car can genuinely reach
   * and leave.
   */
  private final CarReachableVertexSnapper carReachableVertexSnapper;
  private final CarpoolStopIndex stopIndex;

  /**
   * Creates a new carpooling service with the specified dependencies.
   * <p>
   * The service is initialized with standard pre- and post-filters; both filter sets are
   * hardcoded today and could be made configurable in future versions.
   *
   * @param repository provides access to active driver trips with their resolved street vertices,
   *        must not be null
   * @param streetLimitationParametersService provides street routing configuration including
   *        speed limits, must not be null
   * @param vertexCreationService creates request-scoped, bidirectionally-linked temporary vertices
   *        from coordinates, must not be null
   * @param carReachableVertexSnapper snaps passenger-side locations onto car-reachable vertices,
   *        must not be null
   * @param stopIndex the transit stops with their car-reachable snaps, must not be null
   * @throws NullPointerException if any parameter is null
   */
  public DefaultCarpoolingService(
    CarpoolingRepository repository,
    StreetLimitationParametersService streetLimitationParametersService,
    VertexCreationService vertexCreationService,
    CarReachableVertexSnapper carReachableVertexSnapper,
    CarpoolStopIndex stopIndex
  ) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.streetLimitationParametersService = Objects.requireNonNull(
      streetLimitationParametersService,
      "streetLimitationParametersService"
    );
    this.preFilters = TripPreFilters.defaults();
    this.postFilters = ItineraryPostFilters.defaults();
    this.itineraryMapper = new CarpoolItineraryMapper();
    this.positionFinder = new InsertionPositionFinder(
      new BeelineEstimator(streetLimitationParametersService.maxCarSpeed())
    );
    this.vertexCreationService = Objects.requireNonNull(
      vertexCreationService,
      "vertexCreationService"
    );
    this.carReachableVertexSnapper = Objects.requireNonNull(
      carReachableVertexSnapper,
      "carReachableVertexSnapper"
    );
    this.stopIndex = Objects.requireNonNull(stopIndex, "stopIndex");
  }

  /**
   * Routes a direct carpool trip from the passenger's origin to destination.
   * <p>
   * This method executes the full four-phase carpooling algorithm:
   * <ol>
   *   <li><strong>Pre-filtering:</strong> All trips from the repository are filtered by capacity,
   *       time window, and distance to quickly eliminate incompatible matches.</li>
   *   <li><strong>Position finding:</strong> For each surviving trip, viable pickup/dropoff
   *       insertion positions are identified using beeline heuristics (no routing).</li>
   *   <li><strong>Insertion evaluation:</strong> Viable positions are evaluated with A* street
   *       routing to find the insertion that minimizes additional driver travel time while
   *       respecting delay constraints.</li>
   *   <li><strong>Post-filtering:</strong> Routed itineraries are re-checked against tight time
   *       bounds that the loose pre-filter could not enforce.</li>
   * </ol>
   *
   * @param request the routing request. Must have {@link StreetMode#CARPOOL} as the direct mode.
   * @return a list of carpool itineraries, or an empty list if no viable matches are found
   *         or the direct mode is not CARPOOL
   * @throws RoutingValidationException if origin or destination coordinates are missing
   */
  @Override
  public List<Itinerary> routeDirect(RouteRequest request) throws RoutingValidationException {
    if (!StreetMode.CARPOOL.equals(request.journey().direct().mode())) {
      return Collections.emptyList();
    }

    validateRequest(request);

    var carpoolingRequest = CarpoolingRequest.of(request);

    LOG.debug(
      "Finding carpool itineraries from {} to {} at {}",
      carpoolingRequest.getPassengerPickup(),
      carpoolingRequest.getPassengerDropoff(),
      carpoolingRequest.getRequestedDateTime()
    );

    var allTrips = repository.getCarpoolTrips();
    LOG.debug("Repository contains {} carpool trips", allTrips.size());

    var candidateTrips = allTrips
      .stream()
      .filter(trip -> preFilters.isCandidateTrip(trip.trip(), carpoolingRequest))
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
      var router = new CarpoolStreetRouter(streetLimitationParametersService);

      var streetVertexUtils = new StreetVertexUtils(
        this.vertexCreationService,
        temporaryVerticesContainer
      );

      var stopDuration = request.preferences().car().pickupTime();
      var maxWalkToCarpool = carpoolingRequest.getMaxWalkTime();
      var streetSearchRequest = StreetSearchRequestMapper.map(request).build();

      var passengerPickupVertex = streetVertexUtils.createPassengerVertex(
        carpoolingRequest.getPassengerPickup()
      );
      var passengerDropoffVertex = streetVertexUtils.createPassengerVertex(
        carpoolingRequest.getPassengerDropoff()
      );
      if (passengerPickupVertex == null || passengerDropoffVertex == null) {
        LOG.info("Could not link passenger origin/destination to graph");
        return List.of();
      }

      var pickupSnap = carReachableVertexSnapper.snapPickup(
        streetSearchRequest,
        passengerPickupVertex,
        maxWalkToCarpool
      );
      var dropoffSnap = carReachableVertexSnapper.snapDropoff(
        streetSearchRequest,
        passengerDropoffVertex,
        maxWalkToCarpool
      );
      if (pickupSnap == null || dropoffSnap == null) {
        LOG.debug(
          "No car-reachable pickup/dropoff reachable within {} from passenger origin/destination",
          maxWalkToCarpool
        );
        return List.of();
      }

      var insertionEvaluator = new InsertionEvaluator(router, stopDuration);

      var snappedPickup = new WgsCoordinate(pickupSnap.vertex().getCoordinate());
      var snappedDropoff = new WgsCoordinate(dropoffSnap.vertex().getCoordinate());

      var insertionCandidates = candidateTrips
        .stream()
        .map(routableTrip -> {
          var trip = routableTrip.trip();
          List<InsertionPosition> viablePositions = positionFinder.findViablePositions(
            trip,
            snappedPickup,
            snappedDropoff,
            stopDuration
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

          return insertionEvaluator.findBestInsertion(
            routableTrip,
            viablePositions,
            new PassengerSnap(
              pickupSnap.vertex(),
              dropoffSnap.vertex(),
              pickupSnap.walkPath(),
              dropoffSnap.walkPath()
            )
          );
        })
        .filter(Objects::nonNull)
        .toList();

      LOG.debug("Found {} viable insertion candidates", insertionCandidates.size());

      var carpoolReluctance = request.preferences().car().reluctance();
      itineraries = insertionCandidates
        .stream()
        .map(candidate ->
          itineraryMapper.toItinerary(candidate, carpoolReluctance, request.from(), request.to())
        )
        .filter(Objects::nonNull)
        .filter(itinerary -> postFilters.isValidItinerary(itinerary, carpoolingRequest))
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
   *   <li>Takes the trips whose corridor may serve the passenger from a spatial index over the
   *       corridors, and pre-filters them using time and distance heuristic.</li>
   *   <li>Takes the transit stops each trip can serve from its {@link CarpoolCorridor}, keeping
   *       those the passenger can walk to or from within the walk budget.</li>
   *   <li>For each candidate trip and corridor stop combination, identifies viable insertion
   *       positions using beeline heuristics.</li>
   *   <li>Evaluates viable positions with the driving times of the corridor and the passenger's
   *       two street trees, via {@link CorridorRouter}.</li>
   *   <li>Converts the best insertions into {@link CarpoolAccessEgress} objects with timing
   *       information relative to {@code transitSearchTimeZero} for Raptor integration.</li>
   * </ol>
   *
   * @param request the routing request
   * @param streetRequest the street routing parameters for the access or egress leg
   * @param accessOrEgress whether this is an access leg (origin to transit) or egress leg
   *        (transit to destination)
   * @param transitServiceResolver used for resolving stop locations
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
    TransitServiceResolver transitServiceResolver,
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
    var carpoolingRequest = CarpoolingRequest.of(request, accessOrEgress);

    GenericLocation passengerLocation = accessOrEgress.isAccess() ? request.from() : request.to();
    WgsCoordinate passengerCoordinates = passengerLocation.wgsCoordinate();
    double maxCarSpeed = streetLimitationParametersService.maxCarSpeed();
    // A carpool leg is an access/egress leg: the request's maximum duration for the mode applies.
    long maxLegSeconds = request
      .preferences()
      .street()
      .accessEgress()
      .maxDuration()
      .valueOf(StreetMode.CARPOOL)
      .toSeconds();

    var nearbyTrips = repository.getCarpoolTripsNear(passengerCoordinates);
    LOG.debug("{} carpool trips near {}", nearbyTrips.size(), passengerCoordinates);

    var candidateTrips = nearbyTrips
      .stream()
      .filter(trip -> trip.corridor().mayServe(trip.vertices(), passengerCoordinates, maxCarSpeed))
      .filter(trip -> preFilters.isCandidateTrip(trip.trip(), carpoolingRequest))
      .toList();

    if (candidateTrips.isEmpty()) {
      return List.of();
    }

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetVertexUtils = new StreetVertexUtils(
        this.vertexCreationService,
        temporaryVerticesContainer
      );

      var streetSearchRequest = StreetSearchRequestMapper.map(request).build();
      var maxWalkToCarpool = carpoolingRequest.getMaxWalkTime();
      Vertex passengerAccessEgressVertex = streetVertexUtils.createPassengerVertex(
        passengerCoordinates
      );

      if (passengerAccessEgressVertex == null) {
        LOG.info("Could not link passenger coordinates {} to graph", passengerCoordinates);
        return List.of();
      }

      var passengerSnap = accessOrEgress.isEgress()
        ? carReachableVertexSnapper.snapDropoff(
            streetSearchRequest,
            passengerAccessEgressVertex,
            maxWalkToCarpool
          )
        : carReachableVertexSnapper.snapPickup(
            streetSearchRequest,
            passengerAccessEgressVertex,
            maxWalkToCarpool
          );
      if (passengerSnap == null) {
        LOG.debug(
          "No car-reachable vertex reachable within {} from passenger coords {}",
          maxWalkToCarpool,
          passengerCoordinates
        );
        return List.of();
      }
      var passengerVertex = passengerSnap.vertex();

      // The passenger's two trees, outward and inward, sized to the widest leg any candidate trip
      // can insert the passenger into, are the only street searches of the request.
      var passengerRouter = new CarpoolTreeStreetRouter();
      passengerRouter.addVertex(
        passengerVertex,
        CarpoolTreeStreetRouter.Direction.FROM,
        passengerTreeLimit(passengerVertex, candidateTrips, maxCarSpeed, true)
      );
      passengerRouter.addVertex(
        passengerVertex,
        CarpoolTreeStreetRouter.Direction.TO,
        passengerTreeLimit(passengerVertex, candidateTrips, maxCarSpeed, false)
      );
      var corridorRouter = new CorridorRouter(
        passengerRouter,
        passengerVertex,
        new CarpoolStreetRouter(streetLimitationParametersService)
      );

      var stopDuration = request.preferences().car().pickupTime();

      var insertionEvaluator = new InsertionEvaluator(corridorRouter, stopDuration);

      // TODO carpooling currently reuses the car-mode reluctance; revisit whether it should have
      //   its own preference.
      var carpoolReluctance = request.preferences().car().reluctance();

      var stopSnaps = new HashMap<FeedScopedId, Optional<SnapResult>>();
      var accessEgresses = new ArrayList<CarpoolAccessEgress>();
      for (var trip : candidateTrips) {
        var viableStops = registerCorridor(
          trip,
          accessOrEgress,
          passengerSnap,
          stopSnaps,
          streetSearchRequest,
          maxWalkToCarpool,
          stopDuration,
          corridorRouter,
          transitServiceResolver
        );
        var tripWithViableAccessEgress = new TripWithViableAccessEgress(trip, viableStops);
        for (var candidate : insertionEvaluator.findBestInsertions(tripWithViableAccessEgress)) {
          var accessEgress = createCarpoolAccessEgress(
            candidate,
            transitSearchTimeZero,
            carpoolReluctance,
            accessOrEgress,
            passengerLocation
          );
          if (accessEgress.durationInSeconds() > maxLegSeconds) {
            continue;
          }
          accessEgresses.add(accessEgress);
        }
      }
      LOG.debug(
        "{} carpool {} candidates from {} trips",
        accessEgresses.size(),
        accessOrEgress,
        candidateTrips.size()
      );
      return accessEgresses;
    }
  }

  /**
   * Registers the trip's corridor on the router: its baseline legs, and the driving times to and
   * from every corridor stop the passenger can walk to (access) or from (egress) within the walk
   * budget. Returns those stops with the insertion positions the beeline pre-check admits, one
   * entry per stop even when several legs serve it.
   */
  private List<ViableAccessEgress> registerCorridor(
    RoutableCarpoolTrip trip,
    AccessEgressType accessOrEgress,
    SnapResult passengerSnap,
    Map<FeedScopedId, Optional<SnapResult>> stopSnaps,
    StreetSearchRequest streetSearchRequest,
    Duration maxWalk,
    Duration stopDuration,
    CorridorRouter router,
    TransitServiceResolver transitServiceResolver
  ) {
    boolean access = accessOrEgress.isAccess();
    CarpoolCorridor corridor = trip.corridor();
    List<Vertex> waypoints = trip.vertices();

    router.beginTrip();
    for (int leg = 0; leg < corridor.legCount(); leg++) {
      router.register(
        waypoints.get(leg),
        waypoints.get(leg + 1),
        (int) corridor.legDurations().get(leg).toSeconds()
      );
    }
    var snapByStop = new LinkedHashMap<FeedScopedId, SnapResult>();
    for (var stop : corridor.stops()) {
      if (access ? !stop.servesDropoff() : !stop.servesPickup()) {
        continue;
      }
      var snap = stopSnaps
        .computeIfAbsent(stop.stopId(), id -> stopSnap(id, access, streetSearchRequest))
        .orElse(null);
      if (snap == null || GraphPathUtils.durationOrZero(snap.walkPath()).compareTo(maxWalk) > 0) {
        continue;
      }
      router.register(
        waypoints.get(stop.leg()),
        snap.vertex(),
        access ? stop.dropoffToStopSeconds() : stop.pickupToStopSeconds()
      );
      router.register(
        snap.vertex(),
        waypoints.get(stop.leg() + 1),
        access ? stop.dropoffFromStopSeconds() : stop.pickupFromStopSeconds()
      );
      snapByStop.putIfAbsent(stop.stopId(), snap);
    }

    var viable = new ArrayList<ViableAccessEgress>(snapByStop.size());
    for (var entry : snapByStop.entrySet()) {
      var stop = transitServiceResolver.getStopLocation(entry.getKey());
      // AreaStops are GTFS Flex zones — their linked vertex is a synthetic point inside the zone,
      // not a real stop or platform a carpool driver could drop the passenger at, so skip them.
      if (stop instanceof AreaStop) {
        continue;
      }
      var snap = entry.getValue();
      var pickupSide = access ? passengerSnap : snap;
      var dropoffSide = access ? snap : passengerSnap;

      var viablePositions = positionFinder.findViablePositions(
        trip.trip(),
        new WgsCoordinate(pickupSide.vertex().getCoordinate()),
        new WgsCoordinate(dropoffSide.vertex().getCoordinate()),
        stopDuration
      );
      if (viablePositions.isEmpty()) {
        continue;
      }
      viable.add(
        new ViableAccessEgress(
          stop,
          snap.vertex(),
          passengerSnap.vertex(),
          accessOrEgress,
          viablePositions,
          pickupSide.walkPath(),
          dropoffSide.walkPath()
        )
      );
    }
    return viable;
  }

  /**
   * The stop's snap with its walk timed with the request's street preferences (the index times it
   * with the defaults), or empty when the stop cannot be served: no car-reachable vertex, or a walk
   * the passenger may not take, for instance by wheelchair.
   */
  private Optional<SnapResult> stopSnap(
    FeedScopedId stopId,
    boolean access,
    StreetSearchRequest streetSearchRequest
  ) {
    var snap = access ? stopIndex.dropoffSnap(stopId) : stopIndex.pickupSnap(stopId);
    if (snap == null) {
      return Optional.empty();
    }
    if (snap.walkPath() == null) {
      return Optional.of(snap);
    }
    var walk = GraphPathUtils.replay(snap.walkPath(), streetSearchRequest);
    return walk == null ? Optional.empty() : Optional.of(new SnapResult(snap.vertex(), walk));
  }

  /**
   * How far the passenger's tree has to reach. The forward tree answers {@code passenger → X}
   * for a passenger picked up (or, for egress, dropped off) inside some leg {@code k}: the driver
   * goes {@code a_k → passenger → … → a_(k+1)} in at most the leg's limit, and the stretch before
   * the passenger takes at least the beeline from {@code a_k} at the fastest speed in the graph,
   * so the tree has to cover at most the limit minus that bound. The reverse tree answers
   * {@code X → passenger} and subtracts the beeline from the passenger to {@code a_(k+1)} instead.
   * The result is the largest such value over all legs of all candidate trips.
   */
  private static Duration passengerTreeLimit(
    Vertex passenger,
    List<RoutableCarpoolTrip> trips,
    double maxCarSpeed,
    boolean forward
  ) {
    var limit = Duration.ZERO;
    for (var trip : trips) {
      var forTrip = passengerTreeLimit(
        passenger,
        trip.vertices(),
        trip.corridor().legLimits(),
        maxCarSpeed,
        forward
      );
      if (forTrip.compareTo(limit) > 0) {
        limit = forTrip;
      }
    }
    return limit;
  }

  /** The widest leg of one trip, floored at zero. Package-private for testing. */
  static Duration passengerTreeLimit(
    Vertex passenger,
    List<Vertex> waypoints,
    List<Duration> legLimits,
    double maxCarSpeed,
    boolean forward
  ) {
    var limit = Duration.ZERO;
    for (int leg = 0; leg < legLimits.size(); leg++) {
      var beeline = forward
        ? beelineSeconds(waypoints.get(leg), passenger, maxCarSpeed)
        : beelineSeconds(passenger, waypoints.get(leg + 1), maxCarSpeed);
      var needed = legLimits.get(leg).minus(beeline);
      if (needed.compareTo(limit) > 0) {
        limit = needed;
      }
    }
    return limit;
  }

  /** A lower bound on the drive time between two vertices: the beeline at the fastest speed. */
  private static Duration beelineSeconds(Vertex from, Vertex to, double maxCarSpeed) {
    double meters =
      SphericalDistanceLibrary.fastDistance(from.getCoordinate(), to.getCoordinate()) *
      SphericalDistanceLibrary.MAX_ERR_INV;
    return Duration.ofSeconds((long) Math.floor(meters / maxCarSpeed));
  }

  private void validateRequest(RouteRequest request) throws RoutingValidationException {
    Objects.requireNonNull(request.from());
    Objects.requireNonNull(request.to());
    if (request.from().wgsCoordinate() == null) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.FROM_PLACE))
      );
    }
    if (request.to().wgsCoordinate() == null) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.TO_PLACE))
      );
    }
  }

  private CarpoolAccessEgress createCarpoolAccessEgress(
    InsertionCandidate insertionCandidate,
    ZonedDateTime transitSearchTimeZero,
    double carpoolReluctance,
    AccessEgressType accessOrEgress,
    GenericLocation passengerLocation
  ) {
    var carpoolPickupTime = insertionCandidate
      .trip()
      .startTime()
      .plus(insertionCandidate.getDurationUntilPickupArrival());
    var passengerStartTime = carpoolPickupTime.minus(
      GraphPathUtils.durationOrZero(insertionCandidate.walkToPickup())
    );

    var passengerDepartureTime = TimeUtils.toTransitTimeSeconds(
      transitSearchTimeZero,
      passengerStartTime.toInstant()
    );

    StopLocation transitStopLocation = insertionCandidate.transitStop();
    EndpointLabel stopLabel = EndpointLabel.forStop(transitStopLocation);
    EndpointLabel passengerLabel = EndpointLabel.forLocation(passengerLocation);

    EndpointLabel startLabel = accessOrEgress.isAccess() ? passengerLabel : stopLabel;
    EndpointLabel endLabel = accessOrEgress.isAccess() ? stopLabel : passengerLabel;

    return new CarpoolAccessEgress(
      transitStopLocation.getIndex(),
      passengerDepartureTime,
      insertionCandidate,
      TimeAndCost.ZERO,
      carpoolReluctance,
      startLabel,
      endLabel
    );
  }
}
