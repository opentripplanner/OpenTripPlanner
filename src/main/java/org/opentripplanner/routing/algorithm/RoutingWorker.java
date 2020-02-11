package org.opentripplanner.routing.algorithm;

import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.routing.RoutingResponse;
import org.opentripplanner.model.routing.TripSearchMetadata;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilterChainBuilder;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.ItinerariesHelper;
import org.opentripplanner.routing.algorithm.mapping.RaptorPathToItineraryMapper;
import org.opentripplanner.routing.algorithm.mapping.TripPlanMapper;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.TransferToAccessEgressLegMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.RaptorRequestMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.opentripplanner.routing.impl.GraphPathFinder;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Does a complete transit search, including access and egress legs.
 */
public class RoutingWorker {

    private static final int TRANSIT_SEARCH_RANGE_IN_DAYS = 2;
    private static final Logger LOG = LoggerFactory.getLogger(RoutingWorker.class);
    private static final RaptorService<TripSchedule> raptorService = new RaptorService<>(
            // TODO OTP2 - Load turning parameters from config file
            new RaptorTuningParameters() {});

    /**
     * To avoid long searches witch might degrade the performance we use an upper limit
     * to the distance for none transit what we would allow.
     */
    private static final double MAX_WALK_DISTANCE = 50_000;
    private static final double MAX_BIKE_DISTANCE = 150_000;
    private static final double MAX_CAR_DISTANCE = 500_000;

    /** Filter itineraries down to this limit, but not below. */
    private static final int MIN_NUMBER_OF_ITINERARIES = 3;

    /** Never return more that this limit of itineraries. */
    private static final int MAX_NUMBER_OF_ITINERARIES = 200;

    private final RoutingRequest request;
    private TripSearchMetadata responseMetadata = null;
    private Instant filterOnLatestDepartureTime = null;

    public RoutingWorker(RoutingRequest request) {
        this.request = request;
    }

    public RoutingResponse route(Router router) {
        try {
            List<Itinerary> itineraries;

            // Street routing
            itineraries = new ArrayList<>(routeOnStreetGraph(router));

            // Transit routing
            itineraries.addAll(routeTransit(router));

            long startTimeFiltering = System.currentTimeMillis();
            // Filter itineraries
            itineraries = filterChain().filter(itineraries);
            LOG.debug("Filtering took {} ms", System.currentTimeMillis() - startTimeFiltering);

            LOG.debug("Return TripPlan with {} itineraries", itineraries.size());
            return new RoutingResponse(
                    TripPlanMapper.mapTripPlan(request, itineraries),
                    responseMetadata
            );
        }
        finally {
            request.cleanup();
        }
    }

    private List<Itinerary> routeOnStreetGraph(Router router) {
        try {
            if (!request.modes.getNonTransitSet().isValid()) {
                return Collections.emptyList();
            }
            if(!streetDistanceIsReasonable()) { return Collections.emptyList(); }

            RoutingRequest nonTransitRequest = request.clone();
            nonTransitRequest.modes.setTransit(false);

            // we could also get a persistent router-scoped GraphPathFinder but there's no setup cost here
            GraphPathFinder gpFinder = new GraphPathFinder(router);
            List<GraphPath> paths = gpFinder.graphPathFinderEntryPoint(nonTransitRequest);

            // Convert the internal GraphPaths to itineraries
            List<Itinerary> response = GraphPathToItineraryMapper.mapItineraries(paths, request);
            ItinerariesHelper.decorateItinerariesWithRequestData(response, request);
            return response;
        }
        catch (PathNotFoundException e) {
            return Collections.emptyList();
        }
    }

    private Collection<Itinerary> routeTransit(Router router) {
        if (!request.modes.isTransit()) { return Collections.emptyList(); }

        long startTime = System.currentTimeMillis();

        TransitLayer transitLayer = request.ignoreRealtimeUpdates
            ? router.graph.getTransitLayer()
            : router.graph.getRealtimeTransitLayer();

        RaptorRoutingRequestTransitData requestTransitDataProvider;
        requestTransitDataProvider = new RaptorRoutingRequestTransitData(
                transitLayer,
                request.getDateTime().toInstant(),
                TRANSIT_SEARCH_RANGE_IN_DAYS,
                request.modes,
                request.walkSpeed
        );
        LOG.debug("Filtering tripPatterns took {} ms", System.currentTimeMillis() - startTime);

        /* Prepare access/egress transfers */

        double startTimeAccessEgress = System.currentTimeMillis();

        Map<Stop, Transfer> accessTransfers = AccessEgressRouter.streetSearch(request, false, 2000);
        Map<Stop, Transfer> egressTransfers = AccessEgressRouter.streetSearch(request, true, 2000);

        LOG.debug("Access/egress routing took {} ms",
                System.currentTimeMillis() - startTimeAccessEgress
        );

        /* Prepare transit search */

        double startTimeRouting = System.currentTimeMillis();

        TransferToAccessEgressLegMapper accessEgressLegMapper = new TransferToAccessEgressLegMapper(
                transitLayer,
                request.walkSpeed
        );
        RaptorRequest<TripSchedule> raptorRequest = RaptorRequestMapper.mapRequest(
                request,
                requestTransitDataProvider.getStartOfTime(),
                accessEgressLegMapper.map(accessTransfers),
                accessEgressLegMapper.map(egressTransfers)
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
        builder.setMinLimit(Math.min(request.numItineraries, MIN_NUMBER_OF_ITINERARIES));
        builder.setMaxLimit(Math.min(request.numItineraries, MAX_NUMBER_OF_ITINERARIES));
        builder.setGroupByTransferCost(request.walkBoardCost + request.transferPenalty);
        builder.setLatestDepartureTimeLimit(filterOnLatestDepartureTime);

        if(request.debugItineraryFilter) {
            builder.debug();
        }

        return builder.build();
    }

    private boolean streetDistanceIsReasonable() {
        // TODO This currently only calculates the distances between the first fromVertex
        //      and the first toVertex
        double distance = SphericalDistanceLibrary.distance(
                request.rctx.fromVertices
                        .iterator()
                        .next()
                        .getCoordinate(),
                request.rctx.toVertices.iterator().next().getCoordinate()
        );
        return distance < calculateDistanceMaxLimit();
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
                searchWindow,
                time0.plusSeconds(timeOffset - searchWindow).toInstant(),
                time0.plusSeconds(timeOffset + searchWindow).toInstant()
        );
    }

    private double calculateDistanceMaxLimit() {
        double limit = request.maxWalkDistance * 2;
        double maxLimit = request.modes.getCar()
                ? MAX_CAR_DISTANCE
                : (request.modes.getBicycle() ? MAX_BIKE_DISTANCE : MAX_WALK_DISTANCE);

        // Handle overflow and default setting is set to Double MAX_VALUE
        // Everything above Long.MAX_VALUE is treated as Infinite
        if(limit< 0 || limit > Long.MAX_VALUE) {
            LOG.warn(
                "The max walk/bike/car distance is reduced to {} km from Infinite",
                (long)maxLimit/1000
            );
            return maxLimit;
        }

        if (limit > maxLimit) {
            LOG.warn(
                    "The max walk/bike/car distance is reduced to {} km from {} km",
                    (long)maxLimit/1000, (long)limit/1000
            );
            return maxLimit;
        }

        return limit;
    }
}
