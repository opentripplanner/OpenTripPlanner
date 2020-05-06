package org.opentripplanner.routing.algorithm;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.api.response.TripSearchMetadata;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.DirectStreetRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
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

    private static final int TRANSIT_SEARCH_RANGE_IN_DAYS = 2;
    private static final Logger LOG = LoggerFactory.getLogger(RoutingWorker.class);

    private final RaptorService<TripSchedule> raptorService;

    /** Filter itineraries down to this limit, but not below. */
    private static final int MIN_NUMBER_OF_ITINERARIES = 3;

    /** Never return more that this limit of itineraries. */
    private static final int MAX_NUMBER_OF_ITINERARIES = 200;

    private final RoutingRequest request;
    private TripSearchMetadata responseMetadata = null;
    private Instant filterOnLatestDepartureTime = null;

    public RoutingWorker(RaptorConfig<TripSchedule> config, RoutingRequest request) {
        this.raptorService = new RaptorService<>(config);
        this.request = request;
    }

    public RoutingResponse route(Router router) {
        List<Itinerary> itineraries = new ArrayList<>();
        List<RoutingError> routingErrors = new ArrayList<>();

        // Direct street routing
        try {
            itineraries.addAll(DirectStreetRouter.route(router, request));
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        }

        // Transit routing
        try {
            itineraries.addAll(routeTransit(router));
        } catch (RoutingValidationException e) {
            routingErrors.addAll(e.getRoutingErrors());
        }

        // Filter itineraries
        long startTimeFiltering = System.currentTimeMillis();
        itineraries = filterChain().filter(itineraries);
        LOG.debug("Filtering took {} ms", System.currentTimeMillis() - startTimeFiltering);
        LOG.debug("Return TripPlan with {} itineraries", itineraries.size());

        return new RoutingResponse(
                TripPlanMapper.mapTripPlan(request, itineraries),
                responseMetadata,
                routingErrors
        );
    }

    private Collection<Itinerary> routeTransit(Router router) {
        request.rctx.checkIfVerticesFound();
        if (request.modes.transitModes.isEmpty()) { return Collections.emptyList(); }

        long startTime = System.currentTimeMillis();

        TransitLayer transitLayer = request.ignoreRealtimeUpdates
            ? router.graph.getTransitLayer()
            : router.graph.getRealtimeTransitLayer();

        RaptorRoutingRequestTransitData requestTransitDataProvider;
        requestTransitDataProvider = new RaptorRoutingRequestTransitData(
                transitLayer,
                request.getDateTime().toInstant(),
                TRANSIT_SEARCH_RANGE_IN_DAYS,
                request.modes.transitModes,
                request.rctx.bannedRoutes,
                request.walkSpeed
        );
        LOG.debug("Filtering tripPatterns took {} ms", System.currentTimeMillis() - startTime);

        /* Prepare access/egress transfers */

        double startTimeAccessEgress = System.currentTimeMillis();

        Collection<AccessEgress> accessTransfers = AccessEgressRouter.streetSearch(request, false, 2000, transitLayer.getStopIndex());
        Collection<AccessEgress> egressTransfers = AccessEgressRouter.streetSearch(request, true, 2000, transitLayer.getStopIndex());

        LOG.debug("Access/egress routing took {} ms",
                System.currentTimeMillis() - startTimeAccessEgress
        );
        verifyEgressAccess(accessTransfers, egressTransfers);

        /* Prepare transit search */

        double startTimeRouting = System.currentTimeMillis();


        RaptorRequest<TripSchedule> raptorRequest = RaptorRequestMapper.mapRequest(
                request,
                requestTransitDataProvider.getStartOfTime(),
                accessTransfers,
                egressTransfers
        );

        // Route transit
        RaptorResponse<TripSchedule> transitResponse = raptorService.route(raptorRequest, requestTransitDataProvider);

        LOG.debug("Found {} transit itineraries", transitResponse.paths().size());
        LOG.debug("Transit search params used: {}", transitResponse.requestUsed().searchParams());
        LOG.debug("Main routing took {} ms", System.currentTimeMillis() - startTimeRouting);

        /* Create itineraries */

        double startItineraries = System.currentTimeMillis();

        RaptorPathToItineraryMapper itineraryMapper = new RaptorPathToItineraryMapper(
                transitLayer,
                requestTransitDataProvider.getStartOfTime(),
                request,
                accessTransfers,
                egressTransfers
        );
        FareService fareService = request.getRoutingContext().graph.getService(FareService.class);

        List<Itinerary> itineraries = new ArrayList<>();
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

        setResponseMetadata(requestTransitDataProvider, transitResponse);

        // Filter itineraries away that depart after the latest-departure-time for depart after
        // search. These itineraries is a result of timeshifting the access leg and is needed for
        // the raptor to prune the results. These itineraries are often not ideal, but if they
        // pareto optimal for the "next" window, they will appear when a "next" search is performed.
        int win = transitResponse.requestUsed().searchParams().searchWindowInSeconds();
        if(!request.arriveBy && win > 0) {
            filterOnLatestDepartureTime = Instant.ofEpochSecond(request.dateTime + win);
        }

        LOG.debug("Creating {} itineraries took {} ms",
                itineraries.size(),
                System.currentTimeMillis() - startItineraries
        );

        return itineraries;
    }

    private ItineraryFilter filterChain() {
        ItineraryFilterChainBuilder builder = new ItineraryFilterChainBuilder();
        builder.setApproximateMinLimit(Math.min(request.numItineraries, MIN_NUMBER_OF_ITINERARIES));
        builder.setMaxLimit(Math.min(request.numItineraries, MAX_NUMBER_OF_ITINERARIES));
        builder.setGroupByTransferCost(request.walkBoardCost + request.transferCost);
        builder.setLatestDepartureTimeLimit(filterOnLatestDepartureTime);

        if(request.debugItineraryFilter) {
            builder.debug();
        }

        return builder.build();
    }

    private void setResponseMetadata(
            RaptorRoutingRequestTransitData transitData,
            RaptorResponse<TripSchedule> response
    ) {

        SearchParams sp = response.requestUsed().searchParams();
        int searchWindow = sp.searchWindowInSeconds();

        // No results found or standard range-raptor search performed (not multi-criteria)
        if(searchWindow <= 0) { return; }


        ZonedDateTime time0 = transitData.getStartOfTime();
        int timeOffset = request.arriveBy ? sp.latestArrivalTime() : sp.earliestDepartureTime();

        responseMetadata = new TripSearchMetadata(
                Duration.ofSeconds(searchWindow),
                time0.plusSeconds(timeOffset - searchWindow).toInstant(),
                time0.plusSeconds(timeOffset + searchWindow).toInstant()
        );
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
}
