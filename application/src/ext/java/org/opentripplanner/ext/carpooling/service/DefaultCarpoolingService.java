package org.opentripplanner.ext.carpooling.service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.filter.CarpoolingRequest;
import org.opentripplanner.ext.carpooling.filter.ClosestCandidateTrips;
import org.opentripplanner.ext.carpooling.filter.ItineraryPostFilters;
import org.opentripplanner.ext.carpooling.filter.TripPreFilters;
import org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.CarpoolCorridor;
import org.opentripplanner.ext.carpooling.routing.CarpoolStopIndex;
import org.opentripplanner.ext.carpooling.routing.CarpoolStreetRouter;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.ext.carpooling.routing.CorridorRouter;
import org.opentripplanner.ext.carpooling.routing.EndpointLabel;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.routing.InsertionEvaluator;
import org.opentripplanner.ext.carpooling.routing.PassengerSnap;
import org.opentripplanner.ext.carpooling.routing.PerStopCandidateCap;
import org.opentripplanner.ext.carpooling.routing.RoutedSegment;
import org.opentripplanner.ext.carpooling.routing.ViableAccessEgress;
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
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitServiceResolver;
import org.opentripplanner.utils.time.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link CarpoolingService}: pre-filters the candidate trips, evaluates the best insertion
 * of the passenger into each with the {@link InsertionEvaluator}, and maps the results to
 * itineraries (direct) or {@link CarpoolAccessEgress} legs for Raptor (access/egress).
 *
 * @see CarpoolingService for the interface documentation
 */
public class DefaultCarpoolingService implements CarpoolingService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingService.class);

  private final CarpoolingRepository repository;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final TripPreFilters preFilters = TripPreFilters.defaults();
  private final ItineraryPostFilters postFilters = ItineraryPostFilters.defaults();
  private final CarpoolItineraryMapper itineraryMapper = new CarpoolItineraryMapper();
  private final VertexCreationService vertexCreationService;
  private final CarReachableVertexSnapper carReachableVertexSnapper;
  private final CarpoolStopIndex stopIndex;

  /**
   * @param repository the active driver trips with their resolved street vertices and corridors
   * @param streetLimitationParametersService street routing configuration, notably the maximum
   *        car speed
   * @param vertexCreationService links passenger coordinates to temporary street vertices
   * @param carReachableVertexSnapper snaps passenger locations onto car-reachable vertices
   * @param stopIndex the transit stops with their car-reachable snaps
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
   * Routes a direct carpool trip from the passenger's origin to destination: pre-filters the trips,
   * evaluates the best insertion into each of the closest candidates with goal-directed street
   * searches, and post-filters the itineraries against the tight time bounds.
   *
   * @param request the routing request; must have {@link StreetMode#CARPOOL} as the direct mode
   * @return the carpool itineraries, empty when none is viable or the direct mode is not CARPOOL
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

    var preFiltered = repository
      .getCarpoolTrips()
      .stream()
      .filter(trip -> preFilters.isCandidateTrip(trip.trip(), carpoolingRequest))
      .toList();
    // Each candidate costs several street searches; evaluate only the trips passing closest.
    var candidateTrips = ClosestCandidateTrips.closest(
      preFiltered,
      List.of(carpoolingRequest.getPassengerPickup(), carpoolingRequest.getPassengerDropoff()),
      ClosestCandidateTrips.DEFAULT_MAX_CANDIDATE_TRIPS
    );
    LOG.debug(
      "Evaluating {} of {} candidate carpool trips",
      candidateTrips.size(),
      preFiltered.size()
    );
    if (candidateTrips.isEmpty()) {
      return List.of();
    }

    var itineraries = List.<Itinerary>of();
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetVertexUtils = new StreetVertexUtils(
        vertexCreationService,
        temporaryVerticesContainer
      );
      var streetSearchRequest = StreetSearchRequestMapper.map(request).build();
      var maxWalkToCarpool = carpoolingRequest.getMaxWalkTime();

      var pickupVertex = streetVertexUtils.createPassengerVertex(
        carpoolingRequest.getPassengerPickup()
      );
      var dropoffVertex = streetVertexUtils.createPassengerVertex(
        carpoolingRequest.getPassengerDropoff()
      );
      if (pickupVertex == null || dropoffVertex == null) {
        LOG.info("Could not link passenger origin/destination to graph");
        return List.of();
      }
      var pickupSnap = carReachableVertexSnapper.snapPickup(
        streetSearchRequest,
        pickupVertex,
        maxWalkToCarpool
      );
      var dropoffSnap = carReachableVertexSnapper.snapDropoff(
        streetSearchRequest,
        dropoffVertex,
        maxWalkToCarpool
      );
      if (pickupSnap == null || dropoffSnap == null) {
        LOG.debug(
          "No car-reachable pickup/dropoff within {} of passenger origin/destination",
          maxWalkToCarpool
        );
        return List.of();
      }
      var passengerSnap = new PassengerSnap(
        pickupSnap.vertex(),
        dropoffSnap.vertex(),
        pickupSnap.walkPath(),
        dropoffSnap.walkPath()
      );

      var evaluator = new InsertionEvaluator(
        new CarpoolStreetRouter(streetLimitationParametersService),
        request.preferences().car().pickupTime(),
        streetLimitationParametersService.maxCarSpeed()
      );
      var carpoolReluctance = request.preferences().car().reluctance();
      itineraries = candidateTrips
        .stream()
        .map(trip -> evaluator.findBestInsertion(trip, passengerSnap))
        .filter(Objects::nonNull)
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
   * Routes carpool access legs (origin to transit stops) or egress legs (transit stops to
   * destination). The candidate trips come from the spatial index over the corridors; the
   * passenger's two street trees are the only street searches, everything else is read from the
   * trips' corridors. The best insertion per trip and stop becomes a candidate; a stop with more
   * candidates than Raptor can use is capped, see {@link PerStopCandidateCap}.
   *
   * @param transitServiceResolver resolves the corridor stops' ids to stop locations
   * @param transitSearchTimeZero the reference time of Raptor's relative times
   * @return the {@link CarpoolAccessEgress} legs for Raptor, empty when the request's mode for
   *         the direction is not CARPOOL or nothing is viable
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
    var mode = accessOrEgress.isAccess()
      ? request.journey().access().mode()
      : request.journey().egress().mode();
    if (!StreetMode.CARPOOL.equals(mode)) {
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

    var preFiltered = repository
      .getCarpoolTripsNear(passengerCoordinates)
      .stream()
      .filter(trip -> trip.corridor().mayServe(trip.vertices(), passengerCoordinates, maxCarSpeed))
      .filter(trip -> preFilters.isCandidateTrip(trip.trip(), carpoolingRequest))
      .toList();
    var candidateTrips = ClosestCandidateTrips.closest(
      preFiltered,
      List.of(passengerCoordinates),
      ClosestCandidateTrips.DEFAULT_MAX_CANDIDATE_TRIPS
    );
    LOG.debug(
      "Evaluating {} of {} candidate carpool trips near {}",
      candidateTrips.size(),
      preFiltered.size(),
      passengerCoordinates
    );
    if (candidateTrips.isEmpty()) {
      return List.of();
    }

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetVertexUtils = new StreetVertexUtils(
        vertexCreationService,
        temporaryVerticesContainer
      );
      var streetSearchRequest = StreetSearchRequestMapper.map(request).build();
      var maxWalkToCarpool = carpoolingRequest.getMaxWalkTime();

      Vertex passengerLinked = streetVertexUtils.createPassengerVertex(passengerCoordinates);
      if (passengerLinked == null) {
        LOG.info("Could not link passenger coordinates {} to graph", passengerCoordinates);
        return List.of();
      }
      var passengerSnap = accessOrEgress.isAccess()
        ? carReachableVertexSnapper.snapPickup(
            streetSearchRequest,
            passengerLinked,
            maxWalkToCarpool
          )
        : carReachableVertexSnapper.snapDropoff(
            streetSearchRequest,
            passengerLinked,
            maxWalkToCarpool
          );
      if (passengerSnap == null) {
        LOG.debug(
          "No car-reachable vertex within {} of passenger coordinates {}",
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
      var evaluator = new InsertionEvaluator(corridorRouter, stopDuration, maxCarSpeed);
      // TODO carpooling currently reuses the car-mode reluctance; revisit whether it should have
      //   its own preference.
      var carpoolReluctance = request.preferences().car().reluctance();

      var cap = new PerStopCandidateCap<CarpoolAccessEgress>(
        PerStopCandidateCap.DEFAULT_MAX_PER_STOP,
        accessOrEgress.isAccess(),
        request.arriveBy(),
        TimeUtils.toTransitTimeSeconds(transitSearchTimeZero, request.dateTime()),
        (int) carpoolingRequest.getSearchWindow().toSeconds()
      );
      for (var trip : candidateTrips) {
        var viableStops = registerCorridor(
          trip,
          accessOrEgress,
          passengerSnap,
          maxWalkToCarpool,
          corridorRouter,
          transitServiceResolver
        );
        for (var candidate : evaluator.findBestInsertions(trip, viableStops)) {
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
          cap.add(
            accessEgress,
            accessEgress.stop(),
            accessEgress.getPassengerDepartureTime(),
            accessEgress.getPassengerArrivalTime()
          );
        }
      }

      var kept = cap.kept();
      // The survivors outlive the passenger's trees (Raptor holds them): every segment keeps its
      // edge chain instead of the tree, so the trees can be collected when this method returns.
      for (var accessEgress : kept) {
        accessEgress.insertionCandidate().routeSegments().forEach(RoutedSegment::detach);
      }
      LOG.debug(
        "{} carpool {} candidates from {} trips: {} unusable, {} handed to Raptor",
        cap.added(),
        accessOrEgress,
        candidateTrips.size(),
        cap.unusable(),
        kept.size()
      );
      return kept;
    }
  }

  /**
   * Registers the trip's corridor on the router: its baseline legs, and the driving times to and
   * from every corridor stop the passenger can walk to (access) or from (egress) within the walk
   * budget. Returns those stops, one entry per stop even when several legs serve it.
   */
  private List<ViableAccessEgress> registerCorridor(
    CarpoolTripWithVertices trip,
    AccessEgressType accessOrEgress,
    SnapResult passengerSnap,
    Duration maxWalk,
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
      var snap = access
        ? stopIndex.dropoffSnap(stop.stopId())
        : stopIndex.pickupSnap(stop.stopId());
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
      // AreaStops are GTFS Flex zones: their vertex is a synthetic point inside the zone, not a
      // place a driver could drop the passenger at.
      if (stop instanceof AreaStop) {
        continue;
      }
      var snap = entry.getValue();
      viable.add(
        new ViableAccessEgress(
          stop,
          snap.vertex(),
          passengerSnap.vertex(),
          accessOrEgress,
          access ? passengerSnap.walkPath() : snap.walkPath(),
          access ? snap.walkPath() : passengerSnap.walkPath()
        )
      );
    }
    return viable;
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
    List<CarpoolTripWithVertices> trips,
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

    StopLocation transitStop = Objects.requireNonNull(
      insertionCandidate.transitStop(),
      "an access/egress candidate carries its transit stop"
    );
    EndpointLabel stopLabel = EndpointLabel.forStop(transitStop);
    EndpointLabel passengerLabel = EndpointLabel.forLocation(passengerLocation);
    return new CarpoolAccessEgress(
      transitStop.getIndex(),
      passengerDepartureTime,
      insertionCandidate,
      TimeAndCost.ZERO,
      carpoolReluctance,
      accessOrEgress.isAccess() ? passengerLabel : stopLabel,
      accessOrEgress.isAccess() ? stopLabel : passengerLabel
    );
  }
}
