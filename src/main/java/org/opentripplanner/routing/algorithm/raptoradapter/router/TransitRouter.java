package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.api.path.Path;
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
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.search.TemporaryVerticesContainer;
import org.opentripplanner.util.OTPFeature;

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
    return transitRouter.route();
  }

  private TransitRouterResult route() {
    if (request.journey().transit().modes().isEmpty()) {
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

    var accessEgresses = getAccessEgresses();

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

    Collection<Path<TripSchedule>> paths = transitResponse.paths();

    if (OTPFeature.OptimizeTransfers.isOn()) {
      paths =
        TransferOptimizationServiceConfigurator
          .createOptimizeTransferService(
            transitLayer::getStopByIndex,
            requestTransitDataProvider.stopNameResolver(),
            serverContext.transitService().getTransferService(),
            requestTransitDataProvider,
            transitLayer.getStopBoardAlightCosts(),
            raptorRequest,
            request.preferences().transfer().optimization()
          )
          .optimize(transitResponse.paths());
    }

    // Create itineraries

    RaptorPathToItineraryMapper<TripSchedule> itineraryMapper = new RaptorPathToItineraryMapper(
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

  private AccessEgresses getAccessEgresses() {
    var accessEgressMapper = new AccessEgressMapper();
    var accessList = new ArrayList<DefaultAccessEgress>();
    var egressList = new ArrayList<DefaultAccessEgress>();

    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        serverContext.graph(),
        request,
        request.journey().access().mode(),
        request.journey().egress().mode()
      )
    ) {
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
      isEgress
    );

    var results = new ArrayList<>(accessEgressMapper.mapNearbyStops(nearbyStops, isEgress));

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

  private RaptorRoutingRequestTransitData createRequestTransitDataProvider(
    TransitLayer transitLayer
  ) {
    return new RaptorRoutingRequestTransitData(
      transitLayer,
      transitSearchTimeZero,
      additionalSearchDays.additionalSearchDaysInPast(),
      additionalSearchDays.additionalSearchDaysInFuture(),
      new RouteRequestTransitDataProviderFilter(request, serverContext.transitService()),
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
