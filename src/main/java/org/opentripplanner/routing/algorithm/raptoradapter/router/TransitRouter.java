package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RoutingRequestTransitDataProviderFilter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TransitDataProviderFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.configure.TransferOptimizationServiceConfigurator;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.request.RoutingRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.TemporaryVerticesContainer;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.util.OTPFeature;

public class TransitRouter {

  public static final int NOT_SET = -1;

  private final RoutingRequest request;
  private final RoutingPreferences preferences;
  private final OtpServerRequestContext serverContext;
  private final DebugTimingAggregator debugTimingAggregator;
  private final ZonedDateTime transitSearchTimeZero;
  private final AdditionalSearchDays additionalSearchDays;

  private TransitRouter(
    RoutingRequest request,
    RoutingPreferences preferences,
    OtpServerRequestContext serverContext,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    DebugTimingAggregator debugTimingAggregator
  ) {
    this.request = request;
    this.preferences = preferences;
    this.serverContext = serverContext;
    this.transitSearchTimeZero = transitSearchTimeZero;
    this.additionalSearchDays = additionalSearchDays;
    this.debugTimingAggregator = debugTimingAggregator;
  }

  public static TransitRouterResult route(
    RoutingRequest request,
    RoutingPreferences preferences,
    OtpServerRequestContext serverContext,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    DebugTimingAggregator debugTimingAggregator
  ) {
    var transitRouter = new TransitRouter(
      request,
      preferences,
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

    var transitLayer = preferences.transit().ignoreRealtimeUpdates()
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
      preferences,
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
            preferences.transfer().optimization()
          )
          .optimize(transitResponse.paths());
    }

    // Create itineraries

    RaptorPathToItineraryMapper itineraryMapper = new RaptorPathToItineraryMapper(
      serverContext.graph(),
      serverContext.transitService(),
      transitLayer,
      transitSearchTimeZero,
      request,
      preferences
    );

    var itineraries = paths.stream().map(itineraryMapper::createItinerary).toList();

    debugTimingAggregator.finishedItineraryCreation();

    return new TransitRouterResult(itineraries, transitResponse.requestUsed().searchParams());
  }

  private AccessEgresses getAccessEgresses() {
    var accessEgressMapper = new AccessEgressMapper();
    var accessList = new ArrayList<AccessEgress>();
    var egressList = new ArrayList<AccessEgress>();

    var accessCalculator = (Runnable) () -> {
      debugTimingAggregator.startedAccessCalculating();
      accessList.addAll(getAccessEgresses(accessEgressMapper, false));
      debugTimingAggregator.finishedAccessCalculating();
    };

    var egressCalculator = (Runnable) () -> {
      debugTimingAggregator.startedEgressCalculating();
      egressList.addAll(getAccessEgresses(accessEgressMapper, true));
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

    verifyAccessEgress(accessList, egressList);

    return new AccessEgresses(accessList, egressList);
  }

  private Collection<AccessEgress> getAccessEgresses(
    AccessEgressMapper accessEgressMapper,
    boolean isEgress
  ) {
    var results = new ArrayList<AccessEgress>();

    var mode = isEgress ? request.journey().egress().mode() : request.journey().access().mode();

    // Prepare access/egress lists
    var requestAndPreferences = request.getStreetSearchRequestAndPreferences(mode, preferences);
    var accessRequest = requestAndPreferences.getLeft();
    var accessPreferences = requestAndPreferences.getRight();
    try (
      var temporaryVertices = new TemporaryVerticesContainer(
        serverContext.graph(),
        accessRequest,
        accessPreferences
      )
    ) {
      var routingContext = new RoutingContext(
        accessRequest,
        accessPreferences,
        serverContext.graph(),
        temporaryVertices
      );

      if (!isEgress) {
        accessRequest.journey().rental().setAllowKeepingVehicleAtDestination(true);
      }

      var nearbyStops = AccessEgressRouter.streetSearch(
        routingContext,
        serverContext.transitService(),
        mode,
        isEgress
      );

      results.addAll(accessEgressMapper.mapNearbyStops(nearbyStops, isEgress));

      // Special handling of flex accesses
      if (OTPFeature.FlexRouting.isOn() && mode == StreetMode.FLEXIBLE) {
        var flexAccessList = FlexAccessEgressRouter.routeAccessEgress(
          routingContext,
          serverContext.transitService(),
          additionalSearchDays,
          serverContext.routerConfig().flexParameters(accessPreferences),
          isEgress
        );

        results.addAll(accessEgressMapper.mapFlexAccessEgresses(flexAccessList, isEgress));
      }
    }

    return results;
  }

  private RaptorRoutingRequestTransitData createRequestTransitDataProvider(
    TransitLayer transitLayer
  ) {
    var requestAndPreferences = Transfer.prepareTransferRoutingRequest(request, preferences);

    var transferRoutingRequest = requestAndPreferences.getLeft();
    var transferRoutingPreferences = requestAndPreferences.getRight();

    return new RaptorRoutingRequestTransitData(
      transitLayer,
      transitSearchTimeZero,
      additionalSearchDays.additionalSearchDaysInPast(),
      additionalSearchDays.additionalSearchDaysInFuture(),
      createRequestTransitDataProviderFilter(serverContext.transitService()),
      new RoutingContext(
        transferRoutingRequest,
        transferRoutingPreferences,
        serverContext.graph(),
        (Vertex) null,
        null
      )
    );
  }

  private TransitDataProviderFilter createRequestTransitDataProviderFilter(
    TransitService transitService
  ) {
    return new RoutingRequestTransitDataProviderFilter(request, preferences, transitService);
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
