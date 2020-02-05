package org.opentripplanner.routing.algorithm.raptor.router;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.router.street.TransferToAccessEgressLegMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.services.FareService;
import org.opentripplanner.transit.raptor.RaptorService;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.raptor.api.transit.TransferLeg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Does a complete transit search, including access and egress legs.
 * <p>
 * TODO OTP2 - Rename to better reflect scope, does more than Raptor routing. This is - THE
 * router...
 */
public class RaptorRouter {

    private static final int TRANSIT_SEARCH_RANGE_IN_DAYS = 2;
    private static final Logger LOG = LoggerFactory.getLogger(RaptorRouter.class);
    private static final RaptorService<TripSchedule> raptorService = new RaptorService<>(
            // TODO OTP2 - Load turning parameters from config file
            new RaptorTuningParameters() {}
  );

    private final RaptorRoutingRequestTransitData requestTransitDataProvider;
    private final TransitLayer transitLayer;
    private final RoutingRequest request;

    //TODO Naming
    public RaptorRouter(RoutingRequest request, TransitLayer transitLayer) {
        double startTime = System.currentTimeMillis();

    this.requestTransitDataProvider = new RaptorRoutingRequestTransitData(
        transitLayer,
                request.getDateTime().toInstant(),
                TRANSIT_SEARCH_RANGE_IN_DAYS,
                request.modes,
                request.walkSpeed
        );
        LOG.info("Filtering tripPatterns took {} ms", System.currentTimeMillis() - startTime);
        this.transitLayer = transitLayer;
        this.request = request;
    }

    public Collection<Itinerary> route() {

        /* Prepare access/egress transfers */

        double startTimeAccessEgress = System.currentTimeMillis();

        Map<Stop, Transfer> accessTransfers = AccessEgressRouter.streetSearch(request, false, 2000);
        Map<Stop, Transfer> egressTransfers = AccessEgressRouter.streetSearch(request, true, 2000);

        TransferToAccessEgressLegMapper accessEgressLegMapper = new TransferToAccessEgressLegMapper(
                transitLayer
        );

        Collection<TransferLeg> accessTimes = accessEgressLegMapper.map(
                accessTransfers,
                request.walkSpeed
        );
        Collection<TransferLeg> egressTimes = accessEgressLegMapper.map(
                egressTransfers,
                request.walkSpeed
        );

        LOG.info(
                "Access/egress routing took {} ms",
                System.currentTimeMillis() - startTimeAccessEgress
        );

        /* Prepare transit search */

        double startTimeRouting = System.currentTimeMillis();

        RaptorRequestBuilder<TripSchedule> builder = new RaptorRequestBuilder<>();

        int time = DateMapper.secondsSinceStartOfTime(
                requestTransitDataProvider.getStartOfTime(),
                request.getDateTime().toInstant()
        );

        if(request.arriveBy) {
            builder.searchParams().latestArrivalTime(time);
        }
        else {
            builder.searchParams().earliestDepartureTime(time);
        }

        // TODO Expose parameters
        // TODO Remove parameters from API
        builder.profile(RaptorProfile.MULTI_CRITERIA)
                .enableOptimization(Optimization.PARETO_CHECK_AGAINST_DESTINATION)
                .searchParams()
                .searchWindow(request.searchWindow)
                .addAccessStops(accessTimes)
                .addEgressStops(egressTimes)
                .boardSlackInSeconds(request.boardSlack)
                .allowWaitingBetweenAccessAndTransit(false)
                .timetableEnabled(true);

        RaptorRequest<TripSchedule> raptorRequest = builder.build();

        // Route transit
        RaptorResponse<TripSchedule> response = raptorService.route(
                raptorRequest,
                this.requestTransitDataProvider
        );

        LOG.info("Found {} itineraries", response.paths().size());

        LOG.info("Main routing took {} ms", System.currentTimeMillis() - startTimeRouting);

        /* Create itineraries */

        double startItineraries = System.currentTimeMillis();

        ItineraryMapper itineraryMapper = new ItineraryMapper(
                transitLayer,
                requestTransitDataProvider.getStartOfTime(),
                request,
                accessTransfers,
                egressTransfers
        );
        FareService fareService = request.getRoutingContext().graph.getService(FareService.class);

        List<Itinerary> itineraries = new ArrayList<>();
        for (Path<TripSchedule> path : response.paths()) {
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

        LOG.info("Creating {} itineraries took {} ms",
                itineraries.size(),
                System.currentTimeMillis() - startItineraries
        );

        return itineraries;
    }
}
