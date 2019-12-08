package org.opentripplanner.routing.algorithm.raptor.router;

import com.conveyal.r5.otp2.RangeRaptorService;
import com.conveyal.r5.otp2.api.path.Path;
import com.conveyal.r5.otp2.api.request.RangeRaptorProfile;
import com.conveyal.r5.otp2.api.request.RangeRaptorRequest;
import com.conveyal.r5.otp2.api.request.RequestBuilder;
import com.conveyal.r5.otp2.api.request.TuningParameters;
import com.conveyal.r5.otp2.api.transit.TransferLeg;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.DateMapper.secondsSinceStartOfTime;

/**
 * Does a complete transit search, including access and egress legs.
 */
public class RaptorRouter {

  private static final Logger LOG = LoggerFactory.getLogger(RaptorRouter.class);

  private static final RangeRaptorService<TripSchedule> rangeRaptorService = new RangeRaptorService<>(
      // TODO - Load turning parameters from config file
      new TuningParameters() {
      }
  );

  private final RaptorRoutingRequestTransitData otpRRDataProvider;

  private final TransitLayer transitLayer;

  private final RoutingRequest request;

  //TODO Naming
  public RaptorRouter(RoutingRequest request, TransitLayer transitLayer) {
    double startTime = System.currentTimeMillis();
    ZonedDateTime startOfTime = calculateStartOfTime(request);
    this.otpRRDataProvider = new RaptorRoutingRequestTransitData(
        transitLayer, startOfTime, 2, request.modes, request.walkSpeed
    );
    LOG.debug("Filtering tripPatterns took {} ms", System.currentTimeMillis() - startTime);
    this.transitLayer = transitLayer;
    this.request = request;
  }

  public Collection<Itinerary> route() {

    /* Prepare access/egress transfers */

    double startTimeAccessEgress = System.currentTimeMillis();

    Map<Stop, Transfer> accessTransfers =
        AccessEgressRouter.streetSearch(request, false, 2000);
    Map<Stop, Transfer> egressTransfers =
        AccessEgressRouter.streetSearch(request, true, 2000);

    TransferToAccessEgressLegMapper accessEgressLegMapper = new TransferToAccessEgressLegMapper(
        transitLayer);

    Collection<TransferLeg> accessTimes = accessEgressLegMapper
        .map(accessTransfers, request.walkSpeed);
    Collection<TransferLeg> egressTimes = accessEgressLegMapper
        .map(egressTransfers, request.walkSpeed);

    LOG.debug("Access/egress routing took {} ms",
        System.currentTimeMillis() - startTimeAccessEgress);

    /* Prepare transit search */

    double startTimeRouting = System.currentTimeMillis();

    int departureTime = secondsSinceStartOfTime(otpRRDataProvider.getStartOfTime(),
        request.getDateTime().toInstant());

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
    Collection<Path<TripSchedule>> paths = rangeRaptorService
        .route(rangeRaptorRequest, this.otpRRDataProvider);

    LOG.debug("Found {} itineraries", paths.size());

    LOG.debug("Main routing took {} ms", System.currentTimeMillis() - startTimeRouting);

    /* Create itineraries */

    double startItineraries = System.currentTimeMillis();

        ItineraryMapper itineraryMapper = new ItineraryMapper(
                transitLayer,
                otpRRDataProvider.getStartOfTime(),
                request,
                accessTransfers,
                egressTransfers
        );

        FareService fareService = request.getRoutingContext().graph.getService(FareService.class);

        List<Itinerary> itineraries = new ArrayList<>();
        for (Path path : paths) {
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

    LOG.debug("Creating itineraries took {} ms", itineraries.size(),
        System.currentTimeMillis() - startItineraries);

    return itineraries;
  }

  private ZonedDateTime calculateStartOfTime(RoutingRequest request) {
    ZoneId zoneId = request.getRoutingContext().graph.getTimeZone().toZoneId();
    ZonedDateTime zdt = request.getDateTime().toInstant().atZone(zoneId);
    return DateMapper.asStartOfService(zdt);
  }
}
