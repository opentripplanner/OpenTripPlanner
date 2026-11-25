package org.opentripplanner.ext.carpooling.internal;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingParameters;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.corridor.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.corridor.TransitStopIndex;
import org.opentripplanner.ext.carpooling.filter.ClosestCandidateTrips;
import org.opentripplanner.ext.carpooling.filter.ItineraryPostFilters;
import org.opentripplanner.ext.carpooling.filter.SnappedPassenger;
import org.opentripplanner.ext.carpooling.filter.TripPreFilters;
import org.opentripplanner.ext.carpooling.mapping.CarpoolAccessEgress;
import org.opentripplanner.ext.carpooling.mapping.CarpoolItineraryMapper;
import org.opentripplanner.ext.carpooling.routing.CarpoolingRequest;
import org.opentripplanner.ext.carpooling.routing.CorridorRouter;
import org.opentripplanner.ext.carpooling.routing.InsertionCandidate;
import org.opentripplanner.ext.carpooling.routing.InsertionEvaluator;
import org.opentripplanner.ext.carpooling.routing.PassengerRide;
import org.opentripplanner.ext.carpooling.routing.PassengerTrees;
import org.opentripplanner.ext.carpooling.routing.UsableStops;
import org.opentripplanner.ext.carpooling.routing.UsableStops.UsableStop;
import org.opentripplanner.ext.carpooling.street.CarReachableVertexSnapper;
import org.opentripplanner.ext.carpooling.street.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.ext.carpooling.street.CarpoolStreetRouter;
import org.opentripplanner.ext.carpooling.street.PassengerSnapper;
import org.opentripplanner.ext.carpooling.street.StreetPathUtils;
import org.opentripplanner.ext.carpooling.street.TemporaryVertexFactory;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.linking.TemporaryVerticesContainer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.streetadapter.StreetSearchRequestMapper;
import org.opentripplanner.transit.service.TransitServiceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Answers a request in three phases:
 * <ol>
 *   <li>Pre-filtering: once the passenger is snapped to the street network, {@link TripPreFilters}
 *       drop the trips that cannot serve the passenger, and at most
 *       {@link CarpoolingParameters#maxCandidateTripsPerRequest()} of the rest are kept.</li>
 *   <li>Insertion evaluation: the {@link InsertionEvaluator} finds the best insertion of the
 *       passenger into each remaining trip, from the driving times of the trip's corridor and the
 *       passenger's street trees.</li>
 *   <li>Post-filtering, direct routing only: {@link ItineraryPostFilters} check the itineraries
 *       against the passenger's time window.</li>
 * </ol>
 */
public class DefaultCarpoolingService implements CarpoolingService {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultCarpoolingService.class);

  private final CarpoolingRepository repository;
  private final TripPreFilters preFilters;
  private final ItineraryPostFilters postFilters;
  private final CarpoolItineraryMapper itineraryMapper;
  private final VertexCreationService vertexCreationService;
  private final CarReachableVertexSnapper carReachableVertexSnapper;
  private final TransitStopIndex stopIndex;
  private final CarpoolingParameters parameters;
  private final CarpoolStreetRouter pathRouter;

  public DefaultCarpoolingService(
    CarpoolingRepository repository,
    StreetLimitationParametersService streetLimitationParametersService,
    VertexCreationService vertexCreationService,
    CarReachableVertexSnapper carReachableVertexSnapper,
    TransitStopIndex stopIndex,
    CarpoolingParameters parameters
  ) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.preFilters = TripPreFilters.defaults();
    this.postFilters = ItineraryPostFilters.defaults();
    this.itineraryMapper = new CarpoolItineraryMapper();
    this.vertexCreationService = Objects.requireNonNull(
      vertexCreationService,
      "vertexCreationService"
    );
    this.carReachableVertexSnapper = Objects.requireNonNull(
      carReachableVertexSnapper,
      "carReachableVertexSnapper"
    );
    this.stopIndex = Objects.requireNonNull(stopIndex, "stopIndex");
    this.parameters = Objects.requireNonNull(parameters, "parameters");
    Objects.requireNonNull(streetLimitationParametersService, "streetLimitationParametersService");
    this.pathRouter = new CarpoolStreetRouter(
      streetLimitationParametersService,
      parameters.maxTripDuration()
    );
  }

  @Override
  public List<Itinerary> routeDirect(RouteRequest request) throws RoutingValidationException {
    if (!StreetMode.CARPOOL.equals(request.journey().direct().mode())) {
      return Collections.emptyList();
    }

    validateRequest(request);

    var carpoolingRequest = CarpoolingRequest.of(request, parameters.defaultSearchWindow());

    LOG.debug(
      "Finding carpool itineraries from {} to {} at {}",
      carpoolingRequest.getPassengerPickup(),
      carpoolingRequest.getPassengerDropoff(),
      carpoolingRequest.getRequestedDateTime()
    );

    var allTrips = repository.getCarpoolTrips();
    LOG.debug("Repository contains {} carpool trips", allTrips.size());

    if (allTrips.isEmpty()) {
      return List.of();
    }

    var itineraries = List.<Itinerary>of();
    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var passengerSnapper = passengerSnapper(
        temporaryVerticesContainer,
        StreetSearchRequestMapper.map(request).build(),
        carpoolingRequest.getMaxWalkTime()
      );
      var pickupSnap = passengerSnapper.snapPickup(carpoolingRequest.getPassengerPickup());
      var dropoffSnap = passengerSnapper.snapDropoff(carpoolingRequest.getPassengerDropoff());
      if (pickupSnap == null || dropoffSnap == null) {
        return List.of();
      }

      var candidateTrips = candidateTrips(
        allTrips,
        new SnappedPassenger(carpoolingRequest, pickupSnap, dropoffSnap),
        List.of(carpoolingRequest.getPassengerPickup(), carpoolingRequest.getPassengerDropoff())
      );
      if (candidateTrips.isEmpty()) {
        return List.of();
      }

      var passengerRouter = PassengerTrees.directRouter(
        pickupSnap.vertex(),
        dropoffSnap.vertex(),
        candidateTrips,
        parameters.maxTripDuration()
      );
      var corridorRouter = new CorridorRouter(
        passengerRouter,
        List.of(pickupSnap.vertex(), dropoffSnap.vertex()),
        pathRouter
      );
      var insertionEvaluator = new InsertionEvaluator(
        corridorRouter,
        carpoolingRequest.getStopDuration()
      );
      var ride = new PassengerRide(pickupSnap, dropoffSnap, null);

      var insertionCandidates = new ArrayList<InsertionCandidate>();
      for (var trip : candidateTrips) {
        corridorRouter.beginTrip(trip);
        var candidate = insertionEvaluator.findBestInsertion(trip, ride);
        if (candidate != null) {
          insertionCandidates.add(candidate);
        }
      }

      LOG.debug("Found {} viable insertion candidates", insertionCandidates.size());

      var carpoolReluctance = request.preferences().car().reluctance();
      itineraries = insertionCandidates
        .stream()
        .map(candidate ->
          itineraryMapper.toItinerary(
            candidate,
            carpoolReluctance,
            parameters.boardCost(),
            request.from(),
            request.to()
          )
        )
        .filter(itinerary -> postFilters.isValidItinerary(itinerary, carpoolingRequest))
        .toList();
    }

    LOG.info("Returning {} carpool itineraries", itineraries.size());
    return itineraries;
  }

  @Override
  public List<CarpoolAccessEgress> routeAccessEgress(
    RouteRequest request,
    AccessEgressType accessOrEgress,
    TransitServiceResolver transitServiceResolver,
    ZonedDateTime transitSearchTimeZero
  ) throws RoutingValidationException {
    boolean access = accessOrEgress.isAccess();
    var mode = access ? request.journey().access().mode() : request.journey().egress().mode();
    if (!StreetMode.CARPOOL.equals(mode)) {
      return Collections.emptyList();
    }

    validateRequest(request);
    var carpoolingRequest = CarpoolingRequest.of(
      request,
      accessOrEgress,
      parameters.defaultSearchWindow()
    );

    var allTrips = repository.getCarpoolTrips();
    LOG.debug("Repository contains {} carpool trips", allTrips.size());

    if (allTrips.isEmpty()) {
      return List.of();
    }

    GenericLocation passengerLocation = access ? request.from() : request.to();
    WgsCoordinate passengerCoordinates = passengerLocation.wgsCoordinate();
    // A carpool leg is an access/egress leg: the request's maximum duration for the mode applies.
    long maxLegSeconds = request
      .preferences()
      .street()
      .accessEgress()
      .maxDuration()
      .valueOf(StreetMode.CARPOOL)
      .toSeconds();

    try (var temporaryVerticesContainer = new TemporaryVerticesContainer()) {
      var streetSearchRequest = StreetSearchRequestMapper.map(request).build();
      var maxWalkToCarpool = carpoolingRequest.getMaxWalkTime();
      var passengerSnapper = passengerSnapper(
        temporaryVerticesContainer,
        streetSearchRequest,
        maxWalkToCarpool
      );

      // An access passenger is picked up at the origin and dropped off for a transit stop; an
      // egress passenger is picked up from a transit stop and dropped off at the destination.
      boolean dropoffAtStop = access;
      var passengerSnap = access
        ? passengerSnapper.snapPickup(passengerCoordinates)
        : passengerSnapper.snapDropoff(passengerCoordinates);
      if (passengerSnap == null) {
        return List.of();
      }

      var passenger = access
        ? new SnappedPassenger(carpoolingRequest, passengerSnap, null)
        : new SnappedPassenger(carpoolingRequest, null, passengerSnap);
      var candidateTrips = candidateTrips(allTrips, passenger, List.of(passengerCoordinates));

      // Only a trip with a stop the passenger can walk to (access) or from (egress) can serve the
      // request, and only those trips size the passenger's trees.
      var usableStops = new UsableStops(
        stopIndex,
        accessOrEgress,
        streetSearchRequest,
        maxWalkToCarpool
      );
      var servingTrips = new ArrayList<TripStops>();
      for (var trip : candidateTrips) {
        var stops = usableStops.of(trip);
        if (!stops.isEmpty()) {
          servingTrips.add(new TripStops(trip, stops));
        }
      }
      if (servingTrips.isEmpty()) {
        return List.of();
      }
      var trips = servingTrips.stream().map(TripStops::trip).toList();

      // The leg includes the passenger's walk to or from the car, so the ride is that much shorter.
      var maxRide = Duration.ofSeconds(maxLegSeconds).minus(
        StreetPathUtils.durationOrZero(passengerSnap.walkPath())
      );
      var passengerRouter = PassengerTrees.accessEgressRouter(
        passengerSnap.vertex(),
        trips,
        accessOrEgress,
        parameters.maxTripDuration(),
        maxRide
      );
      var corridorRouter = new CorridorRouter(
        passengerRouter,
        List.of(passengerSnap.vertex()),
        pathRouter
      );
      var insertionEvaluator = new InsertionEvaluator(
        corridorRouter,
        carpoolingRequest.getStopDuration()
      );

      // TODO carpooling currently reuses the car-mode reluctance; revisit whether it should have
      //   its own preference.
      var carpoolReluctance = request.preferences().car().reluctance();

      var accessEgresses = new ArrayList<CarpoolAccessEgress>();
      for (var tripStops : servingTrips) {
        var trip = tripStops.trip();
        corridorRouter.beginTrip(trip);
        for (var usable : tripStops.stops()) {
          corridorRouter.registerStop(usable.stop(), usable.snap().vertex(), dropoffAtStop);
        }
        var rides = rides(tripStops.stops(), accessOrEgress, passengerSnap, transitServiceResolver);
        for (var candidate : insertionEvaluator.findBestInsertions(trip, rides)) {
          var accessEgress = CarpoolAccessEgress.of(
            candidate,
            accessOrEgress,
            passengerLocation,
            transitSearchTimeZero,
            carpoolReluctance,
            parameters.boardCost()
          );
          if (accessEgress.durationInSeconds() <= maxLegSeconds) {
            accessEgresses.add(accessEgress);
          }
        }
      }
      LOG.debug(
        "{} carpool {} candidates from {} trips",
        accessEgresses.size(),
        accessOrEgress,
        servingTrips.size()
      );
      return accessEgresses;
    }
  }

  private PassengerSnapper passengerSnapper(
    TemporaryVerticesContainer temporaryVerticesContainer,
    StreetSearchRequest streetSearchRequest,
    Duration maxWalk
  ) {
    return new PassengerSnapper(
      new TemporaryVertexFactory(vertexCreationService, temporaryVerticesContainer),
      carReachableVertexSnapper,
      streetSearchRequest,
      maxWalk
    );
  }

  /**
   * The trips that pass the pre-filters; when more than the request may evaluate, those passing
   * closest to the passenger.
   */
  private List<RoutableCarpoolTrip> candidateTrips(
    Collection<RoutableCarpoolTrip> allTrips,
    SnappedPassenger passenger,
    List<WgsCoordinate> passengerPoints
  ) {
    var preFilteredTrips = allTrips
      .stream()
      .filter(trip -> preFilters.isCandidateTrip(trip, passenger))
      .toList();

    LOG.debug(
      "{} trips passed pre-filters ({} rejected)",
      preFilteredTrips.size(),
      allTrips.size() - preFilteredTrips.size()
    );

    // Every candidate widens the passenger's trees; evaluate only the trips passing closest.
    return ClosestCandidateTrips.closest(
      preFilteredTrips,
      passengerPoints,
      parameters.maxCandidateTripsPerRequest()
    );
  }

  /** A trip with the corridor stops the passenger can use, each with its snap. */
  private record TripStops(RoutableCarpoolTrip trip, List<UsableStop> stops) {}

  /** One ride per stop, even when several legs serve it. */
  private List<PassengerRide> rides(
    List<UsableStop> stops,
    AccessEgressType accessOrEgress,
    SnapResult passengerSnap,
    TransitServiceResolver transitServiceResolver
  ) {
    var snapByStop = new LinkedHashMap<FeedScopedId, SnapResult>();
    for (var usable : stops) {
      snapByStop.putIfAbsent(usable.stop().stopId(), usable.snap());
    }

    var rides = new ArrayList<PassengerRide>(snapByStop.size());
    for (var entry : snapByStop.entrySet()) {
      var stop = transitServiceResolver.getStopLocation(entry.getKey());
      var stopSnap = entry.getValue();
      rides.add(
        accessOrEgress.isAccess()
          ? new PassengerRide(passengerSnap, stopSnap, stop)
          : new PassengerRide(stopSnap, passengerSnap, stop)
      );
    }
    return rides;
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
}
