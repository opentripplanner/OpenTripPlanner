package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.raptor.RaptorService;
import org.opentripplanner.raptor.api.path.RaptorPath;
import org.opentripplanner.raptor.api.response.RaptorResponse;
import org.opentripplanner.raptor.extensions.extrasearch.ExtraMcRouterSearch;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgressPenaltyDecorator;
import org.opentripplanner.routing.algorithm.raptoradapter.router.street.AccessEgresses;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitData;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RoutingAccessEgress;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.DirectTransitRequestMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.DefaultTransitDataProviderFilter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.transferoptimization.configure.TransferOptimizationServiceConfigurator;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.linking.LinkingContext;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.EntityNotFoundException;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;
import org.opentripplanner.transit.model.site.StopLocation;

public class TransitRouter {

  public static final int NOT_SET = -1;

  private final RouteRequest request;
  private final OtpServerRequestContext serverContext;
  private final TransitGroupPriorityService transitGroupPriorityService;
  private final DebugTimingAggregator debugTimingAggregator;
  private final ZonedDateTime transitSearchTimeZero;
  private final AdditionalSearchDays additionalSearchDays;
  private final ViaCoordinateTransferFactory viaTransferResolver;
  private final LinkingContext linkingContext;
  private final CarpoolingService carpoolingService;

  private TransitRouter(
    RouteRequest request,
    OtpServerRequestContext serverContext,
    TransitGroupPriorityService transitGroupPriorityService,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    DebugTimingAggregator debugTimingAggregator,
    LinkingContext linkingContext,
    CarpoolingService carpoolingService
  ) {
    this.request = request;
    this.serverContext = serverContext;
    this.transitGroupPriorityService = transitGroupPriorityService;
    this.transitSearchTimeZero = transitSearchTimeZero;
    this.additionalSearchDays = additionalSearchDays;
    this.debugTimingAggregator = debugTimingAggregator;
    this.viaTransferResolver = serverContext.viaTransferResolver();
    this.linkingContext = linkingContext;
    this.carpoolingService = carpoolingService;
  }

  public static TransitRouterResult route(
    RouteRequest request,
    OtpServerRequestContext serverContext,
    TransitGroupPriorityService priorityGroupConfigurator,
    ZonedDateTime transitSearchTimeZero,
    AdditionalSearchDays additionalSearchDays,
    DebugTimingAggregator debugTimingAggregator,
    LinkingContext linkingContext,
    CarpoolingService carpoolingService
  ) {
    TransitRouter transitRouter = new TransitRouter(
      request,
      serverContext,
      priorityGroupConfigurator,
      transitSearchTimeZero,
      additionalSearchDays,
      debugTimingAggregator,
      linkingContext,
      carpoolingService
    );

    return transitRouter.route();
  }

  private TransitRouterResult route() {
    if (!request.journey().transit().enabled() || request.cannotReachTransit()) {
      return new TransitRouterResult(List.of(), null);
    }

    if (!serverContext.transitService().transitFeedCovers(request.dateTime())) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.OUTSIDE_SERVICE_PERIOD, InputField.DATE_TIME))
      );
    }

    var raptorTransitData = request.preferences().transit().ignoreRealtimeUpdates()
      ? serverContext.transitService().getRaptorTransitData()
      : serverContext.transitService().getRealtimeRaptorTransitData();
    var requestTransitDataProvider = createRequestTransitDataProvider(raptorTransitData);
    var fetchAccessEgress = new AccessEgressFetcher(
      request,
      serverContext,
      transitSearchTimeZero,
      additionalSearchDays,
      linkingContext,
      carpoolingService,
      requestTransitDataProvider
    );

    debugTimingAggregator.finishedPatternFiltering();

    var accessEgresses = fetchAccessEgresses(fetchAccessEgress);

    debugTimingAggregator.finishedAccessEgress(
      accessEgresses.getAccesses().size(),
      accessEgresses.getEgresses().size()
    );

    var extraSearchForSorlandsbanen = createExtraMcRouterSearchForSorlandsbanen(
      accessEgresses,
      raptorTransitData
    );

    // Prepare transit search

    var mapper = RaptorRequestMapper.<TripSchedule>of(
      request,
      transitSearchTimeZero,
      serverContext.raptorConfig().isMultiThreaded(),
      accessEgresses.getAccesses(),
      accessEgresses.getEgresses(),
      serverContext.meterRegistry(),
      viaTransferResolver,
      this::listStopIndexes,
      linkingContext
    );
    var raptorRequest = mapper.mapRaptorRequest();

    // Transit routing using Raptor
    var raptorService = new RaptorService<>(
      serverContext.raptorConfig(),
      extraSearchForSorlandsbanen
    );
    var transitResponse = raptorService.route(raptorRequest, requestTransitDataProvider);

    checkIfTransitConnectionExists(transitResponse);

    Collection<RaptorPath<TripSchedule>> paths = transitResponse.paths();

    debugTimingAggregator.finishedRaptorSearch();

    // Route Direct transit
    var directRequest = DirectTransitRequestMapper.map(
      request,
      transitResponse.requestUsed().searchParams()
    );
    if (directRequest.isPresent()) {
      debugTimingAggregator.startedDirectTransitSearch();
      var directPaths = raptorService.findAllDirectTransit(
        directRequest.get(),
        requestTransitDataProvider
      );
      paths = new ArrayList<>(paths);
      paths.addAll(directPaths);
      debugTimingAggregator.finishedDirectTransitSearch();
    }
    debugTimingAggregator.startedItineraryCreation();

    // TODO VIA - Temporarily turn OptimizeTransfers OFF for VIA search until the service support via
    //            Remove '&& !request.isViaSearch()'
    if (
      OTPFeature.OptimizeTransfers.isOn() &&
      !transitResponse.containsUnknownPaths() &&
      // TODO VIA - This is temporary, we want pass via info in paths so transfer optimizer can
      //            skip legs containing via points.
      request.allowTransferOptimization()
    ) {
      var service = TransferOptimizationServiceConfigurator.createOptimizeTransferService(
        raptorTransitData::getStopByIndex,
        requestTransitDataProvider.stopNameResolver(),
        serverContext.transitService().getConstrainedTransferService(),
        requestTransitDataProvider,
        raptorTransitData.getStopBoardAlightTransferCosts(),
        request.preferences().transfer().optimization(),
        raptorRequest.searchParams().viaLocations()
      );
      paths = service.optimize(paths);
    }

    // Create itineraries

    RaptorPathToItineraryMapper<TripSchedule> itineraryMapper = new RaptorPathToItineraryMapper<>(
      serverContext.graph(),
      serverContext.transitService(),
      serverContext.streetDetailsService(),
      raptorTransitData,
      transitSearchTimeZero,
      request
    );

    List<Itinerary> itineraries = paths.stream().map(itineraryMapper::createItinerary).toList();

    debugTimingAggregator.finishedItineraryCreation();

    return new TransitRouterResult(itineraries, transitResponse.requestUsed().searchParams());
  }

  private AccessEgresses fetchAccessEgresses(AccessEgressFetcher fetchAccessEgress) {
    final var accessList = new ArrayList<RoutingAccessEgress>();
    final var egressList = new ArrayList<RoutingAccessEgress>();

    if (OTPFeature.ParallelRouting.isOn()) {
      try {
        // TODO: This is not using {@link OtpRequestThreadFactory} which mean we do not get
        //       log-trace-parameters-propagation and graceful timeout handling here.
        CompletableFuture.allOf(
          CompletableFuture.runAsync(() -> accessList.addAll(fetchAccess(fetchAccessEgress))),
          CompletableFuture.runAsync(() -> egressList.addAll(fetchEgress(fetchAccessEgress)))
        ).join();
      } catch (CompletionException e) {
        RoutingValidationException.unwrapAndRethrowCompletionException(e);
      }
    } else {
      accessList.addAll(fetchAccess(fetchAccessEgress));
      egressList.addAll(fetchEgress(fetchAccessEgress));
    }

    verifyAccessEgress(accessList, egressList);

    // Decorate access/egress with a penalty to make it less favourable than transit
    var penaltyDecorator = new AccessEgressPenaltyDecorator(
      request.journey().access().mode(),
      request.journey().egress().mode(),
      request.preferences().street().accessEgress().penalty()
    );

    var accessListWithPenalty = penaltyDecorator.decorateAccess(accessList);
    var egressListWithPenalty = penaltyDecorator.decorateEgress(egressList);

    return new AccessEgresses(accessListWithPenalty, egressListWithPenalty);
  }

  private Collection<? extends RoutingAccessEgress> fetchAccess(
    AccessEgressFetcher fetchAccessEgress
  ) {
    debugTimingAggregator.startedAccessCalculating();
    var list = fetchAccessEgress.fetchAccess();
    debugTimingAggregator.finishedAccessCalculating();
    return list;
  }

  private Collection<? extends RoutingAccessEgress> fetchEgress(
    AccessEgressFetcher fetchAccessEgress
  ) {
    debugTimingAggregator.startedEgressCalculating();
    var list = fetchAccessEgress.fetchEgress();
    debugTimingAggregator.finishedEgressCalculating();
    return list;
  }

  private RaptorRoutingRequestTransitData createRequestTransitDataProvider(
    RaptorTransitData raptorTransitData
  ) {
    return new RaptorRoutingRequestTransitData(
      raptorTransitData,
      transitGroupPriorityService,
      transitSearchTimeZero,
      additionalSearchDays.additionalSearchDaysInPast(),
      additionalSearchDays.additionalSearchDaysInFuture(),
      DefaultTransitDataProviderFilter.ofRequest(request),
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

  private IntStream listStopIndexes(FeedScopedId stopLocationId) {
    Collection<StopLocation> stops = serverContext
      .transitService()
      .findStopOrChildStops(stopLocationId);

    if (stops.isEmpty()) {
      throw new EntityNotFoundException(
        "Stop, station, multimodal station or group of stations",
        stopLocationId
      );
    }
    return stops.stream().mapToInt(StopLocation::getIndex);
  }

  /**
   * An optional factory for creating a decorator around the multi-criteria RangeRaptor instance,
   * specifically for the Sorlandsbanen OTP feature.
   */
  @Nullable
  private ExtraMcRouterSearch<TripSchedule> createExtraMcRouterSearchForSorlandsbanen(
    AccessEgresses accessEgresses,
    RaptorTransitData raptorTransitData
  ) {
    if (OTPFeature.Sorlandsbanen.isOff()) {
      return null;
    }
    var service = serverContext.sorlandsbanenService();
    return service == null
      ? null
      : service.createExtraMcRouterSearch(request, accessEgresses, raptorTransitData);
  }
}
