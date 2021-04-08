package org.opentripplanner.routing.algorithm;

import org.opentripplanner.ext.flex.FlexAccessEgress;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.RoutingRequestToFilterChainMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.algorithm.raptor.router.FilterTransitWhenDirectModeIsEmpty;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectFlexRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectStreetRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.FlexAccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.AccessEgressMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RoutingRequestTransitDataProviderFilter;
import org.opentripplanner.routing.algorithm.raptor.transit.request.TransitDataProviderFilter;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.TripSearchMetadata;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.framework.DebugTimingAggregator;
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

    private static final Logger LOG = LoggerFactory.getLogger(RoutingWorker.class);

    private final RaptorService<TripSchedule> raptorService;

    /** An object that accumulates profiling and debugging info for inclusion in the response. */
    public final DebugTimingAggregator debugTimingAggregator = new DebugTimingAggregator();

    private final RoutingRequest request;
    private final FilterTransitWhenDirectModeIsEmpty emptyDirectModeHandler;
    private Instant filterOnLatestDepartureTime = null;
    private int searchWindowUsedInSeconds = NOT_SET;
    private Itinerary firstRemovedItinerary = null;

    public RoutingWorker(RaptorConfig<TripSchedule> config, RoutingRequest request) {
        this.debugTimingAggregator.startedCalculating();
        this.raptorService = new RaptorService<>(config);
        this.request = request;
        this.emptyDirectModeHandler = new FilterTransitWhenDirectModeIsEmpty(request.modes);
    }

    public RoutingResponse route(Router router) {
        List<Itinerary> itineraries = new ArrayList<>();
        List<RoutingError> routingErrors = new ArrayList<>();

        // If no direct mode is set, then we set one.
        // See {@link FilterTransitWhenDirectModeIsEmpty}
        request.modes.directMode = emptyDirectModeHandler.resolveDirectMode();

        this.debugTimingAggregator.finishedPrecalculating();

        // Direct street routing
        try {
            itineraries.addAll(DirectStreetRouter.route(router, request));
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        }

        // Direct flex routing
        if (OTPFeature.FlexRouting.isOn()) {
            try {
                itineraries.addAll(DirectFlexRouter.route(request));
            }
            catch (RoutingValidationException e) {
                routingErrors.addAll(e.getRoutingErrors());
            }
        }

        this.debugTimingAggregator.finishedDirectStreetRouter();

        // Transit routing
        try {
            itineraries.addAll(routeTransit(router));
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        }

        this.debugTimingAggregator.finishedTransitRouter();

        // Filter itineraries
        itineraries = filterItineraries(itineraries);
        LOG.debug("Return TripPlan with {} itineraries", itineraries.size());

        this.debugTimingAggregator.finishedFiltering();

        // Restore original directMode.
        request.modes.directMode = emptyDirectModeHandler.originalDirectMode();

        return new RoutingResponse(
            TripPlanMapper.mapTripPlan(request, itineraries),
            createTripSearchMetadata(),
            routingErrors,
            debugTimingAggregator
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

        RaptorRoutingRequestTransitData requestTransitDataProvider = createRequestTransitDataProvider(transitLayer);

        this.debugTimingAggregator.finishedPatternFiltering();

        AccessEgressMapper accessEgressMapper = new AccessEgressMapper(transitLayer.getStopIndex());
        Collection<AccessEgress> accessList;
        Collection<AccessEgress> egressList;

        // Prepare access/egress lists

        // Special handling of flex accesses
        if (OTPFeature.FlexRouting.isOn() && request.modes.accessMode.equals(StreetMode.FLEXIBLE)) {
            Collection<FlexAccessEgress> flexAccessList = FlexAccessEgressRouter.routeAccessEgress(
                request,
                false
            );
            accessList = accessEgressMapper.mapFlexAccessEgresses(flexAccessList);
        }
        // Regular access routing
        else {
            Collection<NearbyStop> accessStops = AccessEgressRouter.streetSearch(
                request,
                request.modes.accessMode,
                false,
                2000
            );
            accessList = accessEgressMapper.mapNearbyStops(accessStops, false);
        }

        // Special handling of flex egresses
        if (OTPFeature.FlexRouting.isOn() && request.modes.egressMode.equals(StreetMode.FLEXIBLE)) {
            Collection<FlexAccessEgress> flexEgressList = FlexAccessEgressRouter.routeAccessEgress(
                request,
                true
            );
            egressList = accessEgressMapper.mapFlexAccessEgresses(flexEgressList);
        }
        // Regular egress routing
        else {
            Collection<NearbyStop> egressStops = AccessEgressRouter.streetSearch(
                request,
                request.modes.egressMode,
                true,
                2000
            );
            egressList = accessEgressMapper.mapNearbyStops(egressStops, true);
        }

        verifyEgressAccess(accessList, egressList);

        List<Itinerary> itineraries = new ArrayList<>();

        this.debugTimingAggregator.finishedAccessEgress();

        // Prepare transit search
        RaptorRequest<TripSchedule> raptorRequest = RaptorRequestMapper.mapRequest(
                request,
                requestTransitDataProvider.getStartOfTime(),
                accessList,
                egressList
        );

        // Route transit
        RaptorResponse<TripSchedule> transitResponse = raptorService.route(
            raptorRequest,
            requestTransitDataProvider
        );

        LOG.debug("Found {} transit itineraries", transitResponse.paths().size());
        this.debugTimingAggregator.finishedRaptorSearch();

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

        this.debugTimingAggregator.finishedItineraryCreation();

        return itineraries;
    }

    private RaptorRoutingRequestTransitData createRequestTransitDataProvider(
        TransitLayer transitLayer
    ) {
        return new RaptorRoutingRequestTransitData(
                transitLayer,
                request.getDateTime().toInstant(),
                request.additionalSearchDaysAfterToday,
                createRequestTransitDataProviderFilter(),
                request.walkSpeed
        );
    }

    private TransitDataProviderFilter createRequestTransitDataProviderFilter() {
        return new RoutingRequestTransitDataProviderFilter(request);
    }

    private List<Itinerary> filterItineraries(List<Itinerary> itineraries) {
        ItineraryFilter filterChain = RoutingRequestToFilterChainMapper.createFilterChain(
            request,
            filterOnLatestDepartureTime,
            emptyDirectModeHandler.removeWalkAllTheWayResults(),
            it -> firstRemovedItinerary = it
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
