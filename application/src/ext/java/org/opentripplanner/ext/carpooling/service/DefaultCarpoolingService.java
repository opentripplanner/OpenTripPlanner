package org.opentripplanner.ext.carpooling.service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.filter.CarpoolingRequest;
import org.opentripplanner.ext.carpooling.filter.ItineraryPostFilters;
import org.opentripplanner.ext.carpooling.filter.TripPreFilters;
import org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.CarpoolStreetRouter;
import org.opentripplanner.ext.carpooling.routing.CarpoolTreeStreetRouter;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripWithVertices;
import org.opentripplanner.ext.carpooling.routing.EndpointLabel;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.routing.InsertionEvaluator;
import org.opentripplanner.ext.carpooling.routing.InsertionPosition;
import org.opentripplanner.ext.carpooling.routing.InsertionPositionFinder;
import org.opentripplanner.ext.carpooling.routing.PassengerSnap;
import org.opentripplanner.ext.carpooling.routing.TripWithViableAccessEgress;
import org.opentripplanner.ext.carpooling.routing.ViableAccessEgress;
import org.opentripplanner.ext.carpooling.util.BeelineEstimator;
import org.opentripplanner.ext.carpooling.util.CarAccessibleVertexSnapper;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;
import org.opentripplanner.ext.carpooling.util.StreetVertexUtils;
import org.opentripplanner.framework.model.TimeAndCost;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.place.api.NearbyStop;
import org.opentripplanner.place.nearbystopfinder.StreetNearbyStopFinder;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transit.model.site.AreaStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.TransitService;
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

  /**
   * Caps the radius of the nearby-stop search used in access/egress routing. Required to keep
   * computational complexity bounded; remove or widen only after the nearby-stop search is made
   * smarter.
   */
  public static final Duration MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS =
    Duration.ofMinutes(60);
  private final CarpoolingRepository repository;
  private final StreetLimitationParametersService streetLimitationParametersService;
  private final TripPreFilters preFilters;
  private final ItineraryPostFilters postFilters;
  private final CarpoolItineraryMapper itineraryMapper;
  private final InsertionPositionFinder positionFinder;
  private final VertexCreationService vertexCreationService;

  /**
   * Creates a new carpooling service with the specified dependencies.
   * <p>
   * The service is initialized with standard pre- and post-filters; both filter sets are
   * hardcoded today and could be made configurable in future versions.
   *
   * @param repository provides access to active driver trips, must not be null
   * @param streetLimitationParametersService provides street routing configuration including
   *        speed limits, must not be null
   * @param transitService provides timezone from GTFS agency data for time conversions, must not be null
   * @param vertexCreationService creates request-scoped, bidirectionally-linked temporary vertices
   *        from coordinates, must not be null
   * @throws NullPointerException if any parameter is null
   */
  public DefaultCarpoolingService(
    CarpoolingRepository repository,
    StreetLimitationParametersService streetLimitationParametersService,
    TransitService transitService,
    VertexCreationService vertexCreationService
  ) {
    this.repository = repository;
    this.streetLimitationParametersService = streetLimitationParametersService;
    this.preFilters = TripPreFilters.defaults();
    this.postFilters = ItineraryPostFilters.defaults();
    this.itineraryMapper = new CarpoolItineraryMapper();
    this.positionFinder = new InsertionPositionFinder(new BeelineEstimator());
    this.vertexCreationService = vertexCreationService;
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
      .filter(trip -> preFilters.isCandidateTrip(trip, carpoolingRequest))
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
        LOG.warn("Could not link passenger origin/destination to graph");
        return List.of();
      }

      var pickupSnap = CarAccessibleVertexSnapper.snapPickup(
        streetSearchRequest,
        passengerPickupVertex,
        maxWalkToCarpool
      );
      var dropoffSnap = CarAccessibleVertexSnapper.snapDropoff(
        streetSearchRequest,
        passengerDropoffVertex,
        maxWalkToCarpool
      );
      if (pickupSnap == null || dropoffSnap == null) {
        LOG.debug(
          "No car-accessible pickup/dropoff reachable within {} from passenger origin/destination",
          maxWalkToCarpool
        );
        return List.of();
      }

      var insertionEvaluator = new InsertionEvaluator(router, stopDuration);

      var snappedPickup = new WgsCoordinate(pickupSnap.vertex().getCoordinate());
      var snappedDropoff = new WgsCoordinate(dropoffSnap.vertex().getCoordinate());

      var insertionCandidates = candidateTrips
        .stream()
        .map(trip -> {
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

          var tripWithVertices = CarpoolTripWithVertices.create(trip, streetVertexUtils);

          if (tripWithVertices == null) {
            LOG.error("Could not resolve vertices for trip {}", trip.getId());
            return null;
          }

          return insertionEvaluator.findBestInsertion(
            tripWithVertices,
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
   * @param streetRequest the street routing parameters for the access or egress leg
   * @param accessOrEgress whether this is an access leg (origin to transit) or egress leg
   *        (transit to destination)
   * @param transitServiceResolver used for resolving stop locations and nearby stop search
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

    var allTrips = repository.getCarpoolTrips();
    LOG.debug("Repository contains {} carpool trips", allTrips.size());

    GenericLocation passengerLocation = accessOrEgress.isAccess() ? request.from() : request.to();
    WgsCoordinate passengerCoordinates = passengerLocation.wgsCoordinate();

    var candidateTrips = allTrips
      .stream()
      .filter(trip -> preFilters.isCandidateTrip(trip, carpoolingRequest))
      .toList();

    if (candidateTrips.isEmpty()) {
      return List.of();
    }

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetVertexUtils = new StreetVertexUtils(
        this.vertexCreationService,
        temporaryVerticesContainer
      );

      var carpoolTreeVertexRouter = new CarpoolTreeStreetRouter();
      var streetSearchRequest = StreetSearchRequestMapper.map(request).build();
      var maxWalkToCarpool = carpoolingRequest.getMaxWalkTime();
      Vertex passengerAccessEgressVertex = streetVertexUtils.createPassengerVertex(
        passengerCoordinates
      );

      if (passengerAccessEgressVertex == null) {
        LOG.error("Could not link passenger coordinates {} to graph", passengerCoordinates);
        return List.of();
      }

      var passengerSnap = accessOrEgress.isEgress()
        ? CarAccessibleVertexSnapper.snapDropoff(
            streetSearchRequest,
            passengerAccessEgressVertex,
            maxWalkToCarpool
          )
        : CarAccessibleVertexSnapper.snapPickup(
            streetSearchRequest,
            passengerAccessEgressVertex,
            maxWalkToCarpool
          );
      if (passengerSnap == null) {
        LOG.debug(
          "No car-accessible vertex reachable within {} from passenger coords {}",
          maxWalkToCarpool,
          passengerCoordinates
        );
        return List.of();
      }

      var streetNearbyStopFinder = StreetNearbyStopFinder.of(
        null,
        MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS,
        0
      );

      // CAR_PICKUP models a walk → drive → walk chain inside a single A*. Using it here (instead
      // of plain CAR) lets the search find transit stops whose link endpoint is only walk-reachable
      // from the drivable network — typically pedestrian-plaza stops, platforms reached via walk-
      // only tunnels, etc. — which a pure CAR search misses because it cannot leave the car
      // network to walk the final stretch.
      //
      // CAR_PICKUP can return several NearbyStop records per stopId (different paths to the same
      // stop link vertex). They all share the same vertex and stopId — which is everything we read
      // downstream — so any representative works; we don't rank them.
      var foundStops = streetNearbyStopFinder
        .build()
        .findNearbyStops(
          Set.of(passengerSnap.vertex()),
          request,
          StreetMode.CAR_PICKUP,
          accessOrEgress.isEgress()
        );
      // AreaStops are GTFS Flex zones — their linked vertex is a synthetic point inside the zone,
      // not a real curb/platform a carpool driver could drop the passenger at, so skip them.
      var byStopId = new LinkedHashMap<FeedScopedId, NearbyStop>();
      for (var stop : foundStops) {
        if (transitServiceResolver.getStopLocation(stop.stopId) instanceof AreaStop) {
          continue;
        }
        byStopId.putIfAbsent(stop.stopId, stop);
      }
      var stopSnaps = new HashMap<NearbyStop, CarAccessibleVertexSnapper.SnapResult>();
      for (var stop : byStopId.values()) {
        var snap = accessOrEgress.isAccess()
          ? CarAccessibleVertexSnapper.snapDropoff(
              streetSearchRequest,
              stop.state.getVertex(),
              maxWalkToCarpool
            )
          : CarAccessibleVertexSnapper.snapPickup(
              streetSearchRequest,
              stop.state.getVertex(),
              maxWalkToCarpool
            );
        if (snap != null) {
          stopSnaps.put(stop, snap);
        }
      }

      var candidateTripsWithVertices = candidateTrips
        .stream()
        .map(carpoolTrip -> CarpoolTripWithVertices.create(carpoolTrip, streetVertexUtils))
        .filter(Objects::nonNull)
        .toList();

      carpoolTreeVertexRouter.addVertex(
        passengerSnap.vertex(),
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

      var stopDuration = request.preferences().car().pickupTime();

      var insertionEvaluator = new InsertionEvaluator(carpoolTreeVertexRouter, stopDuration);

      var candidateTripsWithViableStopsAndPositions = candidateTripsWithVertices
        .stream()
        .map(tripWithVertices -> {
          var viableSegmentInsertions = stopSnaps
            .entrySet()
            .stream()
            .map(entry -> {
              var nearbyStop = entry.getKey();
              var stopSnap = entry.getValue();
              var pickupSide = accessOrEgress.isAccess() ? passengerSnap : stopSnap;
              var dropoffSide = accessOrEgress.isAccess() ? stopSnap : passengerSnap;

              var viablePositions = positionFinder.findViablePositions(
                tripWithVertices.trip(),
                new WgsCoordinate(pickupSide.vertex().getCoordinate()),
                new WgsCoordinate(dropoffSide.vertex().getCoordinate()),
                stopDuration
              );
              return new ViableAccessEgress(
                nearbyStop,
                stopSnap.vertex(),
                passengerSnap.vertex(),
                accessOrEgress,
                viablePositions,
                pickupSide.walkPath(),
                dropoffSide.walkPath()
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

      // TODO carpooling currently reuses the car-mode reluctance; revisit whether it should have
      //   its own preference.
      var carpoolReluctance = request.preferences().car().reluctance();
      return insertionCandidates
        .stream()
        .map(it ->
          createCarpoolAccessEgress(
            transitServiceResolver,
            it,
            transitSearchTimeZero,
            carpoolReluctance,
            accessOrEgress,
            passengerLocation
          )
        )
        .toList();
    }
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
    TransitServiceResolver transitServiceResolver,
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

    StopLocation transitStopLocation = transitServiceResolver.getStopLocation(
      insertionCandidate.transitStop().stopId
    );
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
