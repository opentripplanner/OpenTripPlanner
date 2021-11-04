package org.opentripplanner.routing.algorithm.raptor.router;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RoutingRequestTransitDataProviderFilter;
import org.opentripplanner.routing.algorithm.raptor.transit.request.TransitDataProviderFilter;
import org.opentripplanner.routing.algorithm.transferoptimization.configure.TransferOptimizationServiceConfigurator;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransitRouter {
    private static final Logger LOG = LoggerFactory.getLogger(TransitRouter.class);

    public static final int NOT_SET = -1;

    private final RoutingRequest request;
    private final Router router;
    private final DebugTimingAggregator debugTimingAggregator;

    private TransitRouter(
            RoutingRequest request,
            Router router,
            DebugTimingAggregator debugTimingAggregator
    ) {
        this.request = request;
        this.router = router;
        this.debugTimingAggregator = debugTimingAggregator;
    }

    private TransitRouterResult route() {
        if (request.modes.transitModes.isEmpty()) {
            return new TransitRouterResult(List.of(), null, NOT_SET);
        }

        if (!router.graph.transitFeedCovers(request.dateTime)) {
            throw new RoutingValidationException(List.of(
                    new RoutingError(RoutingErrorCode.OUTSIDE_SERVICE_PERIOD, InputField.DATE_TIME)
            ));
        }

        var transitLayer = request.ignoreRealtimeUpdates
                ? router.graph.getTransitLayer()
                : router.graph.getRealtimeTransitLayer();

        var requestTransitDataProvider = createRequestTransitDataProvider(
                transitLayer
        );

        debugTimingAggregator.finishedPatternFiltering();

        var accessEgresses = getAccessEgresses(transitLayer);

        debugTimingAggregator.finishedAccessEgress();

        var itineraries = new ArrayList<Itinerary>();

        // Prepare transit search
        var raptorRequest = RaptorRequestMapper.mapRequest(
                request,
                requestTransitDataProvider.getStartOfTime(),
                accessEgresses.getAccesses(),
                accessEgresses.getEgresses()
        );

        // Route transit
        var transitResponse = new RaptorService<>(router.raptorConfig)
                .route(raptorRequest, requestTransitDataProvider);

        checkIfTransitConnectionExists(transitResponse);

        debugTimingAggregator.finishedRaptorSearch();

        Collection<Path<TripSchedule>> paths = transitResponse.paths();

        if(OTPFeature.OptimizeTransfers.isOn()) {
            paths = TransferOptimizationServiceConfigurator.createOptimizeTransferService(
                    transitLayer::getStopByIndex,
                    requestTransitDataProvider.stopNameResolver(),
                    router.graph.getTransferService(),
                    requestTransitDataProvider,
                    raptorRequest,
                    request.transferOptimization
            ).optimize(transitResponse.paths());
        }

        // Create itineraries

        RaptorPathToItineraryMapper itineraryMapper = new RaptorPathToItineraryMapper(
                router.graph,
                transitLayer,
                requestTransitDataProvider.getStartOfTime(),
                request
        );
        FareService fareService = router.graph.getService(FareService.class);

        // TODO
        for (Path<TripSchedule> path : paths) {
            // Convert the Raptor/Astar paths to OTP API Itineraries
            Itinerary itinerary = itineraryMapper.createItinerary(path);
            // Decorate the Itineraries with fare information.
            // Itinerary and Leg are API model classes, lacking internal object references needed for effective
            // fare calculation. We derive the fares from the internal Path objects and add them to the itinerary.
            if (fareService != null) {
                itinerary.fare = fareService.getCost(path, transitLayer);
            }
            itineraries.add(itinerary);
        }

        // Filter itineraries away that depart after the latest-departure-time for depart after
        // search. These itineraries is a result of time-shifting the access leg and is needed for
        // the raptor to prune the results. These itineraries are often not ideal, but if they
        // pareto optimal for the "next" window, they will appear when a "next" search is performed.
        Instant filterOnLatestDepartureTime = null;
        var searchWindowUsedInSeconds = transitResponse.requestUsed().searchParams().searchWindowInSeconds();
        if(!request.arriveBy && searchWindowUsedInSeconds > 0) {
            filterOnLatestDepartureTime = Instant.ofEpochSecond(request.dateTime + searchWindowUsedInSeconds);
        }

        debugTimingAggregator.finishedItineraryCreation();

        return new TransitRouterResult(itineraries, filterOnLatestDepartureTime, searchWindowUsedInSeconds);
    }

    private AccessEgresses getAccessEgresses(
            TransitLayer transitLayer
    ) {
        var accessEgressMapper = new AccessEgressMapper(transitLayer.getStopIndex());
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
                CompletableFuture.allOf(
                        CompletableFuture.runAsync(accessCalculator),
                        CompletableFuture.runAsync(egressCalculator)
                ).join();
            } catch (CompletionException e) {
                if (e.getCause() instanceof RoutingValidationException) {
                    throw (RoutingValidationException) e.getCause();
                } else if (e.getCause() instanceof RuntimeException) {
                    LOG.warn("Unknown exception from access/egress calculation", e.getCause());
                    throw (RuntimeException) e.getCause();
                }
                throw e;
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
        var mode = isEgress ? request.modes.egressMode : request.modes.accessMode;

        // Prepare access/egress lists
        try (RoutingRequest accessRequest = request.getStreetSearchRequest(mode)) {
            accessRequest.setRoutingContext(router.graph);
            if (!isEgress) {
                accessRequest.allowKeepingRentedVehicleAtDestination = false;
            }

            var nearbyStops = AccessEgressRouter.streetSearch(
                    accessRequest,
                    mode,
                    isEgress
            );

            results.addAll(accessEgressMapper.mapNearbyStops(nearbyStops, isEgress));

            // Special handling of flex accesses
            if (OTPFeature.FlexRouting.isOn() && mode == StreetMode.FLEXIBLE) {
                var flexAccessList = FlexAccessEgressRouter.routeAccessEgress(
                        accessRequest,
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
        var graph = router.graph;

        try (RoutingRequest transferRoutingRequest = Transfer.prepareTransferRoutingRequest(request)) {
            transferRoutingRequest.setRoutingContext(graph, (Vertex) null, null);

            return new RaptorRoutingRequestTransitData(
                    graph.getTransferService(),
                    transitLayer,
                    request.getDateTime().toInstant(),
                    request.arriveBy ? request.additionalSearchDaysBeforeToday : 0,
                    request.arriveBy ? 0 : request.additionalSearchDaysAfterToday,
                    createRequestTransitDataProviderFilter(graph.index),
                    transferRoutingRequest
            );
        }
    }

    private TransitDataProviderFilter createRequestTransitDataProviderFilter(GraphIndex graphIndex) {
        return new RoutingRequestTransitDataProviderFilter(request, graphIndex);
    }

    private void verifyAccessEgress(
            Collection<?> access,
            Collection<?> egress
    ) {
        boolean accessExist = !access.isEmpty();
        boolean egressExist = !egress.isEmpty();

        if(accessExist && egressExist) { return; }

        List<RoutingError> routingErrors = new ArrayList<>();
        if (!accessExist) {
            routingErrors.add(
                    new RoutingError(RoutingErrorCode.NO_STOPS_IN_RANGE, InputField.FROM_PLACE));
        }
        if (!egressExist) {
            routingErrors.add(
                    new RoutingError(RoutingErrorCode.NO_STOPS_IN_RANGE, InputField.TO_PLACE));
        }

        throw new RoutingValidationException(routingErrors);
    }

    /**
     * If no paths or search window is found, we assume there is no transit connection between
     * the origin and destination.
     */
    private void checkIfTransitConnectionExists(RaptorResponse<TripSchedule> response) {
        int searchWindowUsed = response.requestUsed().searchParams().searchWindowInSeconds();
        if (searchWindowUsed <= 0 && response.paths().isEmpty()) {
            throw new RoutingValidationException(List.of(
                    new RoutingError(RoutingErrorCode.NO_TRANSIT_CONNECTION, null)));
        }
    }

    public static TransitRouterResult route(
            RoutingRequest request,
            Router router,
            DebugTimingAggregator debugTimingAggregator
    ) {
        return new TransitRouter(request, router, debugTimingAggregator).route();
    }
}
