package org.opentripplanner.routing.algorithm.raptoradapter.router;

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
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouter;
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
  }

  public static TransitRouterResult route(
    RouteRequest request,
    OtpServerRequestContext serverContext,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    DebugTimingAggregator debugTimingAggregator
  ) {
    var transitRouter = new TransitRouter(
      request,
      serverContext,
      transitSearchTimeZero,
      additionalSearchDays,
      debugTimingAggregator
    );
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        serverContext.graph(),
        request,
        request.journey().access().mode(),
        request.journey().egress().mode()
      )
    ) {
      return transitRouter.route(temporaryVertices);
    }
  }

  private TransitRouterResult route(TemporaryVerticesContainer temporaryVertices) {
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

    var accessEgresses = getAccessEgresses(temporaryVertices);

    debugTimingAggregator.finishedAccessEgress(
      accessEgresses.getAccesses().size(),
      accessEgresses.getEgresses().size()
    );

    // Prepare transit search
    var raptorRequest = RaptorRequestMapper.mapRequest(
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
      paths =
        TransferOptimizationServiceConfigurator
          .createOptimizeTransferService(
            transitLayer::getStopByIndex,
            requestTransitDataProvider.stopNameResolver(),
            serverContext.transitService().getTransferService(),
            requestTransitDataProvider,
            transitLayer.getStopBoardAlightCosts(),
            request.preferences().transfer().optimization()
          )
          .optimize(transitResponse.paths());
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

  private AccessEgresses getAccessEgresses(TemporaryVerticesContainer temporaryVertices) {
    var accessEgressMapper = new AccessEgressMapper();
    var accessList = new ArrayList<DefaultAccessEgress>();
    var egressList = new ArrayList<DefaultAccessEgress>();

    var accessCalculator = (Runnable) () -> {
      debugTimingAggregator.startedAccessCalculating();
      accessList.addAll(getAccessEgresses(accessEgressMapper, temporaryVertices, false));
      debugTimingAggregator.finishedAccessCalculating();
    };

    var egressCalculator = (Runnable) () -> {
      debugTimingAggregator.startedEgressCalculating();
      egressList.addAll(getAccessEgresses(accessEgressMapper, temporaryVertices, true));
      debugTimingAggregator.finishedEgressCalculating();
    };

    if (OTPFeature.ParallelRouting.isOn()) {
      try {
        // TODO: This is not using {@link OtpRequestThreadFactory} witch mean we do not get
        //       log-trace-parameters-propagation and graceful timeout handling here.
        CompletableFuture
          .allOf(
            CompletableFuture.runAsync(accessCalculator),
            CompletableFuture.runAsync(egressCalculator)
          )
          .join();
      } catch (CompletionException e) {
        RoutingValidationException.unwrapAndRethrowCompletionException(e);
      }
    } else {
      accessCalculator.run();
      egressCalculator.run();
    }

    verifyAccessEgress(accessList, egressList);

    return new AccessEgresses(accessList, egressList);
  }

  private Collection<DefaultAccessEgress> getAccessEgresses(
    AccessEgressMapper accessEgressMapper,
    TemporaryVerticesContainer temporaryVertices,
    boolean isEgress
  ) {
    var streetRequest = isEgress ? request.journey().egress() : request.journey().access();

    // Prepare access/egress lists
    RouteRequest accessRequest = request.clone();

    if (!isEgress) {
      accessRequest.journey().rental().setAllowArrivingInRentedVehicleAtDestination(false);
    }

    var nearbyStops = AccessEgressRouter.streetSearch(
      accessRequest,
      temporaryVertices,
      serverContext.transitService(),
      streetRequest,
      serverContext.dataOverlayContext(accessRequest),
      isEgress,
      accessRequest.preferences().street().maxAccessEgressDuration().valueOf(streetRequest.mode())
    );

    var isAccess = !isEgress;
    List<DefaultAccessEgress> results = new ArrayList<>(
      accessEgressMapper.mapNearbyStops(nearbyStops, isEgress)
    );
    results = timeshiftRideHailing(streetRequest, isAccess, results);

    // Special handling of flex accesses
    if (OTPFeature.FlexRouting.isOn() && streetRequest.mode() == StreetMode.FLEXIBLE) {
      var flexAccessList = FlexAccessEgressRouter.routeAccessEgress(
        accessRequest,
        temporaryVertices,
        serverContext,
        additionalSearchDays,
        serverContext.flexConfig(),
        serverContext.dataOverlayContext(accessRequest),
        isEgress
      );

      results.addAll(accessEgressMapper.mapFlexAccessEgresses(flexAccessList, isEgress));
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
    boolean isAccess,
    List<DefaultAccessEgress> results
  ) {
    if (streetRequest.mode() == StreetMode.CAR_HAILING) {
      results =
        RideHailingAccessShifter.shiftAccesses(
          isAccess,
          results,
          serverContext.rideHailingServices(),
          request,
          Instant.now()
        );
    }
    return results;
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
    int searchWindowUsed = response.requestUsed().searchParams().searchWindowInSeconds();
    if (searchWindowUsed <= 0 && response.paths().isEmpty()) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.NO_TRANSIT_CONNECTION, null))
      );
    }
  }
}
