package org.opentripplanner.routing.algorithm;

import org.opentripplanner.ext.flex.FlexRouter;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.RoutingRequestToFilterChainMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectStreetRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.TripSearchMetadata;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugAggregator;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.util.OTPFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Does a complete transit search, including access and egress legs.
 * <p>
 * This class has a request scope, hence the "Worker" name.
 */
public class RoutingWorker {
    private static final int NOT_SET = -1;

    /**
     * The numbers of days before the search date to consider when filtering trips for this search.
     * This is set to 1 to account for trips starting yesterday and crossing midnight so that they
     * can be boarded today. If there are trips that last multiple days, this will need to be
     * increased.
     */
    private static final int ADDITIONAL_SEARCH_DAYS_BEFORE_TODAY = 1;

    /**
     * The number of days after the search date to consider when filtering trips for this search.
     * This is set to 1 to account for searches today having a search window that crosses midnight
     * and would also need to board trips starting tomorrow. If a search window that lasts more than
     * a day is used, this will need to be increased.
     */
    private static final int ADDITIONAL_SEARCH_DAYS_AFTER_TODAY = 2;

    private static final Logger LOG = LoggerFactory.getLogger(RoutingWorker.class);

    private final RaptorService<TripSchedule> raptorService;

    /** An object that accumulates profiling and debugging info for inclusion in the response. */
    public final DebugAggregator debugAggregator = new DebugAggregator();

    private final RoutingRequest request;
    private Instant filterOnLatestDepartureTime = null;
    private int searchWindowUsedInSeconds = NOT_SET;
    private Itinerary firstRemovedItinerary = null;

    public RoutingWorker(RaptorConfig<TripSchedule> config, RoutingRequest request) {
        this.debugAggregator.startedCalculating();
        this.raptorService = new RaptorService<>(config);
        this.request = request;
    }

    public RoutingResponse route(Router router) {
        List<Itinerary> itineraries = new ArrayList<>();
        List<RoutingError> routingErrors = new ArrayList<>();

        this.debugAggregator.finishedPrecalculating();

        // Direct street routing
        try {
            itineraries.addAll(DirectStreetRouter.route(router, request));
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        }

        this.debugAggregator.finishedDirectStreetRouter();

        // Transit routing
        try {
            itineraries.addAll(routeTransit(router));
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        }

        this.debugAggregator.finishedTransitRouter();

        // Filter itineraries
        itineraries = filterItineraries(itineraries);
        LOG.debug("Return TripPlan with {} itineraries", itineraries.size());

        this.debugAggregator.finishedFiltering();

        return new RoutingResponse(
            TripPlanMapper.mapTripPlan(request, itineraries),
            createTripSearchMetadata(),
            routingErrors,
            debugAggregator
        );
    }

    private Collection<Itinerary> routeTransit(Router router) {
        request.setRoutingContext(router.graph);
        if (request.modes.transitModes.isEmpty()) { return Collections.emptyList(); }

        if (!router.graph.transitFeedCovers(request.dateTime)) {
            throw new RoutingValidationException(List.of(
                    new RoutingError(RoutingErrorCode.OUTSIDE_SERVICE_PERIOD, InputField.DATE_TIME)
            ));
        }

        TransitLayer transitLayer = request.ignoreRealtimeUpdates
            ? router.graph.getTransitLayer()
            : router.graph.getRealtimeTransitLayer();

        RaptorRoutingRequestTransitData requestTransitDataProvider;
        requestTransitDataProvider = new RaptorRoutingRequestTransitData(
                transitLayer,
                request.getDateTime().toInstant(),
                ADDITIONAL_SEARCH_DAYS_AFTER_TODAY,
                request.modes.transitModes,
                request.rctx.bannedRoutes,
                request.walkSpeed
        );

        this.debugAggregator.finishedPatternFiltering();

        // Prepare access/egress transfers
        Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(request, false, 2000);
        Collection<NearbyStop> egressStops = AccessEgressRouter.streetSearch(request, true, 2000);

        AccessEgressMapper accessEgressMapper = new AccessEgressMapper(transitLayer.getStopIndex());
        Collection<AccessEgress> accessTransfers = accessEgressMapper.mapNearbyStops(accessStops, false);
        Collection<AccessEgress> egressTransfers = accessEgressMapper.mapNearbyStops(egressStops, true);

        List<Itinerary> itineraries = new ArrayList<>();

        if (OTPFeature.FlexRouting.isOn() && request.modes.transitModes.contains(TransitMode.FLEXIBLE)) {
            FlexRouter flexRouter = new FlexRouter(
                request.rctx.graph,
                request.getDateTime().toInstant(),
                request.arriveBy,
                ADDITIONAL_SEARCH_DAYS_BEFORE_TODAY,
                ADDITIONAL_SEARCH_DAYS_AFTER_TODAY,
                accessStops,
                egressStops
            );

            itineraries.addAll(flexRouter.createFlexOnlyItineraries());
            accessTransfers.addAll(accessEgressMapper.mapFlexAccessEgresses(flexRouter.createFlexAccesses()));
            egressTransfers.addAll(accessEgressMapper.mapFlexAccessEgresses(flexRouter.createFlexEgresses()));
        }

        verifyEgressAccess(accessTransfers, egressTransfers);

        this.debugAggregator.finishedAccessEgress();

        // Prepare transit search
        RaptorRequest<TripSchedule> raptorRequest = RaptorRequestMapper.mapRequest(
                request,
                requestTransitDataProvider.getStartOfTime(),
                accessTransfers,
                egressTransfers
        );

        // Route transit
        RaptorResponse<TripSchedule> transitResponse = raptorService.route(
            raptorRequest,
            requestTransitDataProvider
        );

        LOG.debug("Found {} transit itineraries", transitResponse.paths().size());
        LOG.debug("Transit search params used: {}", transitResponse.requestUsed().searchParams());
        this.debugAggregator.finishedRaptorSearch();

        // Create itineraries

        RaptorPathToItineraryMapper itineraryMapper = new RaptorPathToItineraryMapper(
                transitLayer,
                requestTransitDataProvider.getStartOfTime(),
                request
        );
        FareService fareService = request.getRoutingContext().graph.getService(FareService.class);

        for (Path<TripSchedule> path : transitResponse.paths()) {
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

        checkIfTransitConnectionExists(transitResponse);

        // Filter itineraries away that depart after the latest-departure-time for depart after
        // search. These itineraries is a result of time-shifting the access leg and is needed for
        // the raptor to prune the results. These itineraries are often not ideal, but if they
        // pareto optimal for the "next" window, they will appear when a "next" search is performed.
        searchWindowUsedInSeconds = transitResponse.requestUsed().searchParams().searchWindowInSeconds();
        if(!request.arriveBy && searchWindowUsedInSeconds > 0) {
            filterOnLatestDepartureTime = Instant.ofEpochSecond(request.dateTime + searchWindowUsedInSeconds);
        }

        this.debugAggregator.finishedItineraryCreation();

        return itineraries;
    }

    private List<Itinerary> filterItineraries(List<Itinerary> itineraries) {
        ItineraryFilter filterChain = RoutingRequestToFilterChainMapper.createFilterChain(
            request, filterOnLatestDepartureTime, it -> firstRemovedItinerary = it
        );
        return filterChain.filter(itineraries);
    }

    private void verifyEgressAccess(
            Collection<?> access,
            Collection<?> egress
    ) {
        boolean accessExist = !access.isEmpty();
        boolean egressExist = !egress.isEmpty();

        if(accessExist && egressExist) { return; }

        List<RoutingError> routingErrors = new ArrayList<>();
        if(!accessExist) { routingErrors.add(
            new RoutingError(RoutingErrorCode.NO_STOPS_IN_RANGE, InputField.FROM_PLACE));
        }
        if(!egressExist) { routingErrors.add(
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

    private TripSearchMetadata createTripSearchMetadata() {
        if(searchWindowUsedInSeconds == NOT_SET) { return null; }

        Instant reqTime = Instant.ofEpochSecond(request.dateTime);

        if (request.arriveBy) {
            return TripSearchMetadata.createForArriveBy(
                reqTime,
                searchWindowUsedInSeconds,
                firstRemovedItinerary == null
                    ? null
                    : firstRemovedItinerary.endTime().toInstant()
            );
        }
        else {
            return TripSearchMetadata.createForDepartAfter(
                reqTime,
                searchWindowUsedInSeconds,
                firstRemovedItinerary == null
                    ? null
                    : firstRemovedItinerary.startTime().toInstant()
            );
        }
    }
}
