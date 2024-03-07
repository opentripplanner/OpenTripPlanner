package org.opentripplanner.routing.algorithm.raptoradapter.router;

import static org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType.ACCESS;
import static org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType.EGRESS;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.opentripplanner.ext.ridehailing.RideHailingAccessShifter;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.response.RaptorResponse;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressPenaltyDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressType;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgresses;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.DefaultAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RouteRequestTransitDataProviderFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.configure.TransferOptimizationServiceConfigurator;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TemporaryVerticesContainer;

public class TransitRouter {

  public static final int NOT_SET = -1;

  private final RouteRequest request;
  private final OtpServerRequestContext serverContext;
  private final DebugTimingAggregator debugTimingAggregator;
  private final ZonedDateTime transitSearchTimeZero;
  private final AdditionalSearchDays additionalSearchDays;
  private final TemporaryVerticesContainer temporaryVerticesContainer;

  private TransitRouter(
    RouteRequest request,
    OtpServerRequestContext serverContext,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    DebugTimingAggregator debugTimingAggregator
  ) {
    this.request = request;
    this.serverContext = serverContext;
    this.transitSearchTimeZero = transitSearchTimeZero;
    this.additionalSearchDays = additionalSearchDays;
    this.debugTimingAggregator = debugTimingAggregator;
    this.temporaryVerticesContainer = createTemporaryVerticesContainer(request, serverContext);
  }

  public static TransitRouterResult route(
    RouteRequest request,
    OtpServerRequestContext serverContext,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    DebugTimingAggregator debugTimingAggregator
  ) {
    TransitRouter transitRouter = new TransitRouter(
      request,
      serverContext,
      transitSearchTimeZero,
      additionalSearchDays,
      debugTimingAggregator
    );

    return transitRouter.routeAndCleanupAfter();
  }

  private TransitRouterResult routeAndCleanupAfter() {
    // try(auto-close):
    //   Make sure we clean up graph by removing temp-edges from the graph before we exit.
    try (temporaryVerticesContainer) {
      return route();
    }
  }

  private TransitRouterResult route() {
    if (!request.journey().transit().enabled()) {
      return new TransitRouterResult(List.of(), null);
    }

    if (!serverContext.transitService().transitFeedCovers(request.dateTime())) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.OUTSIDE_SERVICE_PERIOD, InputField.DATE_TIME))
      );
    }

    var transitLayer = request.preferences().transit().ignoreRealtimeUpdates()
      ? serverContext.transitService().getTransitLayer()
      : serverContext.transitService().getRealtimeTransitLayer();

    var requestTransitDataProvider = createRequestTransitDataProvider(transitLayer);

    debugTimingAggregator.finishedPatternFiltering();

    var accessEgresses = fetchAccessEgresses();

    debugTimingAggregator.finishedAccessEgress(
      accessEgresses.getAccesses().size(),
      accessEgresses.getEgresses().size()
    );

    // Prepare transit search
    var raptorRequest = RaptorRequestMapper.<TripSchedule>mapRequest(
      request,
      transitSearchTimeZero,
      serverContext.raptorConfig().isMultiThreaded(),
      accessEgresses.getAccesses(),
      accessEgresses.getEgresses(),
      serverContext.meterRegistry()
    );

    // Route transit
    var raptorService = new RaptorService<>(serverContext.raptorConfig());
    var transitResponse = raptorService.route(raptorRequest, requestTransitDataProvider);

    checkIfTransitConnectionExists(transitResponse);

    debugTimingAggregator.finishedRaptorSearch();

    Collection<RaptorPath<TripSchedule>> paths = transitResponse.paths();

    if (OTPFeature.OptimizeTransfers.isOn() && !transitResponse.containsUnknownPaths()) {
      var service = TransferOptimizationServiceConfigurator.createOptimizeTransferService(
        transitLayer::getStopByIndex,
        requestTransitDataProvider.stopNameResolver(),
        serverContext.transitService().getTransferService(),
        requestTransitDataProvider,
        transitLayer.getStopBoardAlightCosts(),
        request.preferences().transfer().optimization(),
        raptorRequest.multiCriteria()
      );
      paths = service.optimize(transitResponse.paths());
    }

    // Create itineraries

    RaptorPathToItineraryMapper<TripSchedule> itineraryMapper = new RaptorPathToItineraryMapper<>(
      serverContext.graph(),
      serverContext.transitService(),
      transitLayer,
      transitSearchTimeZero,
      request
    );

    List<Itinerary> itineraries = paths.stream().map(itineraryMapper::createItinerary).toList();

    debugTimingAggregator.finishedItineraryCreation();

    return new TransitRouterResult(itineraries, transitResponse.requestUsed().searchParams());
  }

  private AccessEgresses fetchAccessEgresses() {
    final var asyncAccessList = new ArrayList<DefaultAccessEgress>();
    final var asyncEgressList = new ArrayList<DefaultAccessEgress>();

    if (OTPFeature.ParallelRouting.isOn()) {
      try {
        // TODO: This is not using {@link OtpRequestThreadFactory} which mean we do not get
        //       log-trace-parameters-propagation and graceful timeout handling here.
        CompletableFuture
          .allOf(
            CompletableFuture.runAsync(() -> asyncAccessList.addAll(fetchAccess())),
            CompletableFuture.runAsync(() -> asyncEgressList.addAll(fetchEgress()))
          )
          .join();
      } catch (CompletionException e) {
        RoutingValidationException.unwrapAndRethrowCompletionException(e);
      }
    } else {
      asyncAccessList.addAll(fetchAccess());
      asyncEgressList.addAll(fetchEgress());
    }

    verifyAccessEgress(asyncAccessList, asyncEgressList);

    // Decorate access/egress with a penalty to make it less favourable than transit
    var penaltyDecorator = new AccessEgressPenaltyDecorator(
      request.journey().access().mode(),
      request.journey().egress().mode(),
      request.preferences().street().accessEgress().penalty()
    );

    var accessList = penaltyDecorator.decorateAccess(asyncAccessList);
    var egressList = penaltyDecorator.decorateEgress(asyncEgressList);

    return new AccessEgresses(accessList, egressList);
  }

  private Collection<DefaultAccessEgress> fetchAccess() {
    debugTimingAggregator.startedAccessCalculating();
    var list = fetchAccessEgresses(ACCESS);
    debugTimingAggregator.finishedAccessCalculating();
    return list;
  }

  private Collection<DefaultAccessEgress> fetchEgress() {
    debugTimingAggregator.startedEgressCalculating();
    var list = fetchAccessEgresses(EGRESS);
    debugTimingAggregator.finishedEgressCalculating();
    return list;
  }

  private Collection<DefaultAccessEgress> fetchAccessEgresses(AccessEgressType type) {
    var streetRequest = type.isAccess() ? request.journey().access() : request.journey().egress();

    // Prepare access/egress lists
    RouteRequest accessRequest = request.clone();

    if (type.isAccess()) {
      accessRequest.withPreferences(p -> {
        p.withBike(b -> b.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false)));
        p.withCar(c -> c.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false)));
        p.withScooter(s -> s.withRental(r -> r.withAllowArrivingInRentedVehicleAtDestination(false))
        );
      });
    }

    Duration durationLimit = accessRequest
      .preferences()
      .street()
      .accessEgress()
      .maxDuration()
      .valueOf(streetRequest.mode());
    int stopCountLimit = accessRequest.preferences().street().accessEgress().maxStopCount();

    var nearbyStops = AccessEgressRouter.streetSearch(
      accessRequest,
      temporaryVerticesContainer,
      serverContext.transitService(),
      streetRequest,
      serverContext.dataOverlayContext(accessRequest),
      type.isEgress(),
      durationLimit,
      stopCountLimit
    );

    List<DefaultAccessEgress> results = new ArrayList<>(
      AccessEgressMapper.mapNearbyStops(nearbyStops, type.isEgress())
    );
    results = timeshiftRideHailing(streetRequest, type, results);

    // Special handling of flex accesses
    if (OTPFeature.FlexRouting.isOn() && streetRequest.mode() == StreetMode.FLEXIBLE) {
      var flexAccessList = FlexAccessEgressRouter.routeAccessEgress(
        accessRequest,
        temporaryVerticesContainer,
        serverContext,
        additionalSearchDays,
        serverContext.flexConfig(),
        serverContext.dataOverlayContext(accessRequest),
        type.isEgress()
      );

      results.addAll(AccessEgressMapper.mapFlexAccessEgresses(flexAccessList, type.isEgress()));
    }

    return results;
  }

  /**
   * Given a list of {@code results} shift the access ones which contain driving
   * so that they only start at the time when the ride hailing vehicle can actually be there
   * to pick up passengers.
   * <p>
   * If there are accesses/egresses with only walking then they remain unchanged.
   * <p>
   * This method is a good candidate to be moved to the access/egress filter chain when that has
   * been added.
   */
  private List<DefaultAccessEgress> timeshiftRideHailing(
    StreetRequest streetRequest,
    AccessEgressType type,
    List<DefaultAccessEgress> accessEgressList
  ) {
    if (streetRequest.mode() != StreetMode.CAR_HAILING) {
      return accessEgressList;
    }
    return RideHailingAccessShifter.shiftAccesses(
      type.isAccess(),
      accessEgressList,
      serverContext.rideHailingServices(),
      request,
      Instant.now()
    );
  }

  private RaptorRoutingRequestTransitData createRequestTransitDataProvider(
    TransitLayer transitLayer
  ) {
    return new RaptorRoutingRequestTransitData(
      transitLayer,
      transitSearchTimeZero,
      additionalSearchDays.additionalSearchDaysInPast(),
      additionalSearchDays.additionalSearchDaysInFuture(),
      new RouteRequestTransitDataProviderFilter(request),
      request
    );
  }

  private void verifyAccessEgress(Collection<?> access, Collection<?> egress) {
    boolean accessExist = !access.isEmpty();
    boolean egressExist = !egress.isEmpty();

    if (accessExist && egressExist) {
      return;
    }

    List<RoutingError> routingErrors = new ArrayList<>();
    if (!accessExist) {
      routingErrors.add(
        new RoutingError(RoutingErrorCode.NO_STOPS_IN_RANGE, InputField.FROM_PLACE)
      );
    }
    if (!egressExist) {
      routingErrors.add(new RoutingError(RoutingErrorCode.NO_STOPS_IN_RANGE, InputField.TO_PLACE));
    }

    throw new RoutingValidationException(routingErrors);
  }

  /**
   * If no paths or search window is found, we assume there is no transit connection between the
   * origin and destination.
   */
  private void checkIfTransitConnectionExists(RaptorResponse<TripSchedule> response) {
    if (response.noConnectionFound()) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.NO_TRANSIT_CONNECTION, null))
      );
    }
  }

  private TemporaryVerticesContainer createTemporaryVerticesContainer(
    RouteRequest request,
    OtpServerRequestContext serverContext
  ) {
    return new TemporaryVerticesContainer(
      serverContext.graph(),
      request,
      request.journey().access().mode(),
      request.journey().egress().mode()
    );
  }
}
