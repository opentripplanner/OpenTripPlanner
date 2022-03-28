package org.opentripplanner.routing.algorithm.raptoradapter.router;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.opentripplanner.model.plan.Itinerary;
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
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.fares.FareService;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
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
    private final ZonedDateTime transitSearchTimeZero;
    private final AdditionalSearchDays additionalSearchDays;


    private TransitRouter(
            RoutingRequest request,
            Router router,
            ZonedDateTime transitSearchTimeZero,
            AdditionalSearchDays additionalSearchDays,
            DebugTimingAggregator debugTimingAggregator
    ) {
        this.request = request;
        this.router = router;
        this.transitSearchTimeZero = transitSearchTimeZero;
        this.additionalSearchDays = additionalSearchDays;
        this.debugTimingAggregator = debugTimingAggregator;
    }

    private TransitRouterResult route() {
        if (request.modes.transitModes.isEmpty()) {
            return new TransitRouterResult(List.of(), null);
        }

        if (!router.graph.transitFeedCovers(request.getDateTime())) {
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

        debugTimingAggregator.finishedAccessEgress(
                accessEgresses.getAccesses().size(),
                accessEgresses.getEgresses().size()
        );

        var itineraries = new ArrayList<Itinerary>();

        // Prepare transit search
        var raptorRequest = RaptorRequestMapper.mapRequest(
                request,
                transitSearchTimeZero,
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
                    transitLayer.getStopIndex().stopBoardAlightCosts,
                    raptorRequest,
                    request.transferOptimization
            ).optimize(transitResponse.paths());
        }

        // Create itineraries

        RaptorPathToItineraryMapper itineraryMapper = new RaptorPathToItineraryMapper(
                router.graph,
                transitLayer,
                transitSearchTimeZero,
                request
        );
        FareService fareService = router.graph.getService(FareService.class);

        for (Path<TripSchedule> path : paths) {
            // Convert the Raptor/Astar paths to OTP API Itineraries
            Itinerary itinerary = itineraryMapper.createItinerary(path);

            // Decorate the Itineraries with fare information.
            if (fareService != null) {
                itinerary.fare = fareService.getCost(itinerary);
            }

            itineraries.add(itinerary);
        }

        debugTimingAggregator.finishedItineraryCreation();

        return new TransitRouterResult(itineraries, transitResponse.requestUsed().searchParams());
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
                        additionalSearchDays,
                        router.routerConfig.flexParameters(request),
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
                    transitSearchTimeZero,
                    additionalSearchDays.additionalSearchDaysInPast(),
                    additionalSearchDays.additionalSearchDaysInFuture(),
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
            ZonedDateTime transitSearchTimeZero,
            AdditionalSearchDays additionalSearchDays,
            DebugTimingAggregator debugTimingAggregator
    ) {
        return new TransitRouter(request, router, transitSearchTimeZero, additionalSearchDays, debugTimingAggregator).route();
    }
}
