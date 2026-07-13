package org.opentripplanner.ext.carpooling.service;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.filter.CarpoolingRequest;
import org.opentripplanner.ext.carpooling.filter.ItineraryPostFilters;
import org.opentripplanner.ext.carpooling.filter.TripPreFilters;
import org.opentripplanner.ext.carpooling.internal.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.routing.CarpoolRouter;
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
   * Hard ceiling on the nearby-stop search radius used in access/egress routing. The search uses
   * the request's {@code accessEgress} max duration for {@link StreetMode#CARPOOL}, but never more
   * than this, so an unusually large preference cannot blow up the search. Lower or remove only
   * once the nearby-stop search is made smarter.
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
    this.positionFinder = new InsertionPositionFinder(
      new BeelineEstimator(streetLimitationParametersService.maxCarSpeed())
    );
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

      // A carpool access/egress leg is an access/egress leg, so the search may not exceed the
      // request's accessEgress max duration: stops beyond that reach only yield legs that violate
      // the cap. Bound it further by MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS so an
      // unusually large preference cannot blow up the search.
      var preferredSearchDuration = request
        .preferences()
        .street()
        .accessEgress()
        .maxDuration()
        .valueOf(StreetMode.CARPOOL);
      var nearbyStopSearchDuration = min(
        preferredSearchDuration,
        MAX_SEARCH_DURATION_FOR_NEARBY_STOPS_FOR_ACCESS_EGRESS
      );

      var streetNearbyStopFinder = StreetNearbyStopFinder.of(null, nearbyStopSearchDuration, 0);

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

      // Sizes each leg's tree from OTP's own routed leg durations (cached per trip) and re-routes
      // any baseline leg the tree misses — see resolveLegDurations and the InsertionEvaluator
      // fallback below.
      var baselineRouter = new CarpoolStreetRouter(streetLimitationParametersService);

      // Each waypoint's tree only has to span its own leg plus the feasible insertion detour —
      // see driverLegTreeLimits.
      var routableTrips = new ArrayList<CarpoolTripWithVertices>(candidateTripsWithVertices.size());
      var passengerTreeLimit = Duration.ZERO;
      for (var tripWithVertices : candidateTripsWithVertices) {
        var legDurations = resolveLegDurations(tripWithVertices, baselineRouter);
        // A trip whose baseline cannot be routed within the carpool bound cannot carry a passenger:
        // skip it before sizing and building trees its baseline would fail to route in anyway.
        if (legDurations == null) {
          continue;
        }
        var legLimits = driverLegTreeLimits(tripWithVertices.trip(), legDurations);
        var vertices = tripWithVertices.vertices();
        for (int leg = 0; leg < legLimits.length; leg++) {
          carpoolTreeVertexRouter.addVertex(
            vertices.get(leg),
            CarpoolTreeStreetRouter.Direction.FROM,
            legLimits[leg]
          );
          carpoolTreeVertexRouter.addVertex(
            vertices.get(leg + 1),
            CarpoolTreeStreetRouter.Direction.TO,
            legLimits[leg]
          );
          passengerTreeLimit = max(passengerTreeLimit, legLimits[leg]);
        }
        routableTrips.add(tripWithVertices);
      }
      // Every passenger segment lies on a single leg of some candidate trip, so the largest leg
      // limit bounds them all. A smaller cap would silently drop feasible insertions: route()
      // never falls back from the passenger's own forward tree.
      carpoolTreeVertexRouter.addVertex(
        passengerSnap.vertex(),
        CarpoolTreeStreetRouter.Direction.BOTH,
        passengerTreeLimit
      );

      var stopDuration = request.preferences().car().pickupTime();

      var insertionEvaluator = new InsertionEvaluator(
        carpoolTreeVertexRouter,
        baselineRouter,
        stopDuration
      );

      var candidateTripsWithViableStopsAndPositions = routableTrips
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

  /**
   * Sizes the street routing tree for each leg of a driver trip from the leg's travel duration:
   * {@code result[k]} limits the leg from waypoint {@code k} to {@code k + 1} — waypoint
   * {@code k}'s forward tree and waypoint {@code k + 1}'s reverse tree — one entry per leg. Sizing
   * per leg instead of to the whole {@link CarpoolTrip#startTime()}→{@link CarpoolTrip#endTime()}
   * span keeps trees local even when consecutive waypoints are minutes apart, the dominant cost
   * for long or multi-waypoint trips.
   * <p>
   * A leg's limit is its travel duration plus a small slack plus the detour
   * allowance: the smallest deviation budget among the stops downstream of the leg. A detour on a
   * leg delays every downstream stop, each checked against its own budget by
   * {@link org.opentripplanner.ext.carpooling.constraints.PassengerDelayConstraints}, so the
   * smallest downstream budget is the most a feasible detour can add — a stop beyond the tree
   * would be rejected by the delay constraints anyway.
   * <p>
   * Each limit is finally capped at {@link CarpoolTrip#MAX_TRIP_DURATION}, the same bound the
   * SIRI mapper rejects over-long trips at. A consistent trip's leg never approaches that bound, so
   * the cap only trims the detour margin of a trip at the very edge of the accepted range; its real
   * purpose is to keep every driver tree bounded should a trip with inconsistent geometry slip past
   * the mapper's checks, so that no single request can expand a multi-hour tree.
   *
   * @param legDurations the travel duration of each leg, one entry per leg (length
   *        {@code stops().size() - 1}). The caller passes OTP's own routed durations (see
   *        {@link #resolveLegDurations}) so the tree is sized against the same routing model it is
   *        built with; a leg's tree then spans its baseline whatever the SIRI schedule claimed.
   */
  static Duration[] driverLegTreeLimits(CarpoolTrip trip, Duration[] legDurations) {
    var stops = trip.stops();
    int n = stops.size();

    // Small slack: a floor for zero-length legs and a guard against rounding or routing-model
    // discrepancy. Not load-bearing — the OTP-measured leg duration plus the downstream deviation
    // budget already span the baseline and any feasible detour.
    var slack = Duration.ofMinutes(1);

    // Detour allowance = backward running minimum of the downstream deviation budgets; the
    // origin's budget never participates — no detour can delay the origin.
    var legLimits = new Duration[n - 1];
    var detourAllowance = stops.get(n - 1).getDeviationBudget();
    for (int k = n - 2; k >= 0; k--) {
      detourAllowance = min(detourAllowance, stops.get(k + 1).getDeviationBudget());
      legLimits[k] = min(
        legDurations[k].plus(slack).plus(detourAllowance),
        CarpoolTrip.MAX_TRIP_DURATION
      );
    }
    return legLimits;
  }

  /**
   * Resolves the per-leg travel durations used to size a trip's routing trees, or {@code null}
   * when the trip's baseline cannot be routed and the trip should be skipped.
   * <p>
   * The outcome is memoized on the repository
   * ({@link CarpoolingRepository#cachedBaselineRouting}) — both success and failure — because the
   * baseline route depends only on the trip's waypoint geometry and the static street graph, never
   * on the passenger request: the baseline router is bounded by
   * {@link CarpoolTrip#MAX_TRIP_DURATION}, a fixed ceiling, not by any request preference. On a
   * cache miss every leg is routed with {@code baselineRouter}; the result — the durations if all
   * legs route, or unroutable if any leg cannot be driven within that ceiling — is cached either
   * way, so a trip that cannot carry a passenger is routed once and skipped cheaply thereafter. The
   * cache validates its entry against the trip's current route points, so a re-routed trip never
   * reads a stale verdict.
   */
  @Nullable
  private Duration[] resolveLegDurations(
    CarpoolTripWithVertices tripWithVertices,
    CarpoolRouter baselineRouter
  ) {
    var trip = tripWithVertices.trip();
    var cached = repository.cachedBaselineRouting(trip);
    if (cached != null) {
      return cached.legDurations();
    }
    var routed = routeBaselineLegDurations(tripWithVertices, baselineRouter);
    repository.cacheBaselineRouting(trip, routed);
    return routed;
  }

  /**
   * Routes every leg of the trip with {@code baselineRouter} and returns OTP's travel duration for
   * each, or {@code null} if any leg cannot be routed. {@code baselineRouter} is a goal-directed
   * search bounded by {@link CarpoolTrip#MAX_TRIP_DURATION} — the same ceiling the trees are capped
   * at — so a {@code null} here means the leg cannot be driven within the carpool bound: no tree
   * could route it either, and the trip is dropped.
   */
  @Nullable
  private static Duration[] routeBaselineLegDurations(
    CarpoolTripWithVertices tripWithVertices,
    CarpoolRouter baselineRouter
  ) {
    var vertices = tripWithVertices.vertices();
    var durations = new Duration[vertices.size() - 1];
    for (int leg = 0; leg < durations.length; leg++) {
      var path = baselineRouter.route(vertices.get(leg), vertices.get(leg + 1));
      if (path == null) {
        LOG.debug(
          "OTP could not route baseline leg {} of trip {} within the carpool bound; skipping it",
          leg,
          tripWithVertices.trip().getId()
        );
        return null;
      }
      durations[leg] = GraphPathUtils.durationOrZero(path);
    }
    return durations;
  }

  private static Duration min(Duration a, Duration b) {
    return a.compareTo(b) <= 0 ? a : b;
  }

  private static Duration max(Duration a, Duration b) {
    return a.compareTo(b) >= 0 ? a : b;
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
