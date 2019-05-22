package org.opentripplanner.routing.algorithm.raptor.router;

import com.conveyal.r5.otp2.RangeRaptorService;
import com.conveyal.r5.otp2.api.path.Path;
import com.conveyal.r5.otp2.api.request.Optimization;
import com.conveyal.r5.otp2.api.request.RangeRaptorProfile;
import com.conveyal.r5.otp2.api.request.RangeRaptorRequest;
import com.conveyal.r5.otp2.api.request.RequestBuilder;
import com.conveyal.r5.otp2.api.request.TuningParameters;
import com.conveyal.r5.otp2.api.transit.TransferLeg;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.itinerary.ItineraryMapper;
import org.opentripplanner.routing.algorithm.raptor.router.street.TransferToAccessEgressLegMapper;
import org.opentripplanner.routing.algorithm.raptor.router.street.AccessEgressRouter;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper;
import org.opentripplanner.routing.algorithm.raptor.transit.request.RaptorRoutingRequestTransitData;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper.secondsSinceStartOfTime;

/**
 * Does a complete transit search, including access and egress legs.
 */
public class RaptorRouter {
    private static final Logger LOG = LoggerFactory.getLogger(RaptorRouter.class);

    private static final RangeRaptorService<TripSchedule> rangeRaptorService = new RangeRaptorService<>(
            // TODO - Load turning parameters from config file
            new TuningParameters() {}
    );

    private final RaptorRoutingRequestTransitData otpRRDataProvider;

    private final TransitLayer transitLayer;

    //TODO Naming
    public RaptorRouter(RoutingRequest request, TransitLayer transitLayer) {
        double startTime = System.currentTimeMillis();
        ZonedDateTime startOfTime = calculateStartOfTime(request);
        this.otpRRDataProvider = new RaptorRoutingRequestTransitData(
                transitLayer, startOfTime, 2, request.modes, request.walkSpeed
        );
        LOG.info("Filtering tripPatterns took {} ms", System.currentTimeMillis() - startTime);
        this.transitLayer = transitLayer;
    }

    public TripPlan route(RoutingRequest request) {

        /* Prepare access/egress transfers */

        double startTimeAccessEgress = System.currentTimeMillis();

        Map<Stop, Transfer> accessTransfers =
            AccessEgressRouter.streetSearch(request, false, 2000);
        Map<Stop, Transfer> egressTransfers =
            AccessEgressRouter.streetSearch(request, true, 2000);

        TransferToAccessEgressLegMapper accessEgressLegMapper = new TransferToAccessEgressLegMapper(transitLayer);

        Collection<TransferLeg> accessTimes = accessEgressLegMapper.map(accessTransfers, request.walkSpeed);
        Collection<TransferLeg> egressTimes = accessEgressLegMapper.map(egressTransfers, request.walkSpeed);

        LOG.info("Access/egress routing took {} ms", System.currentTimeMillis() - startTimeAccessEgress);

        /* Prepare transit search */

        double startTimeRouting = System.currentTimeMillis();

        int departureTime = secondsSinceStartOfTime(otpRRDataProvider.getStartOfTime(), request.getDateTime().toInstant());

        // TODO Expose parameters
        // TODO Remove parameters from API
        RequestBuilder builder = new RequestBuilder();
        builder.profile(RangeRaptorProfile.STANDARD)
                .searchParams()
                .earliestDepartureTime(departureTime)
                .searchWindowInSeconds(request.raptorSearchWindow)
                .addAccessStops(accessTimes)
                .addEgressStops(egressTimes)
                .boardSlackInSeconds(request.boardSlack)
                .timetableEnabled(true);

        //TODO Check in combination with timetableEnabled
        //builder.enableOptimization(Optimization.PARETO_CHECK_AGAINST_DESTINATION);

        RangeRaptorRequest rangeRaptorRequest = builder.build();

        /* Route transit */

        // We know this cast is correct because we have instantiated rangeRaptorService as RangeRaptorService<TripSchedule>
        @SuppressWarnings("unchecked")
        Collection<Path<TripSchedule>> paths = rangeRaptorService.route(rangeRaptorRequest, this.otpRRDataProvider);

        LOG.info("Found {} itineraries", paths.size());

        LOG.info("Main routing took {} ms", System.currentTimeMillis() - startTimeRouting);

        /* Create itineraries */

        double startItineraries = System.currentTimeMillis();

        ItineraryMapper itineraryMapper = new ItineraryMapper(transitLayer, otpRRDataProvider.getStartOfTime(), request, accessTransfers, egressTransfers);

        List<Itinerary> itineraries = paths.stream()
                .map(itineraryMapper::createItinerary)
                .collect(Collectors.toList());

        TripPlan tripPlan = itineraryMapper.createTripPlan(itineraries);

        LOG.info("Creating itineraries took {} ms", System.currentTimeMillis() - startItineraries);
        if (tripPlan.itinerary.isEmpty()) {
            throw new PathNotFoundException();
        }

        return tripPlan;
    }

    private ZonedDateTime calculateStartOfTime(RoutingRequest request) {
        ZoneId zoneId = request.getRoutingContext().graph.getTimeZone().toZoneId();
        ZonedDateTime zdt = request.getDateTime().toInstant().atZone(zoneId);
        return DateMapper.asStartOfService(zdt);
    }
}
