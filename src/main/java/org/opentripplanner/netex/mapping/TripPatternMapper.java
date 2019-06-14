package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.loader.util.HierarchicalMap;
import org.opentripplanner.netex.loader.util.HierarchicalMapById;
import org.opentripplanner.netex.loader.util.HierarchicalMultimap;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.opentripplanner.netex.mapping.FeedScopedIdFactory.createFeedScopedId;

/**
 * Maps NeTEx JourneyPattern to OTP TripPattern. All ServiceJourneys in the same JourneyPattern contain the same
 * sequence of stops. This means that they can all use the same StopPattern. Each ServiceJourney contains
 * TimeTabledPassingTimes that are mapped to StopTimes.
 * <p>
 * Headsigns in NeTEx are only specified once and then valid for each subsequent TimeTabledPassingTime until a new
 * headsign is specified. This is accounted for in the mapper.
 */
class TripPatternMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TripPatternMapper.class);

    private final OtpTransitServiceBuilder transitBuilder;

    private final HierarchicalMapById<Route> routeById;

    private final EntityById<FeedScopedId, org.opentripplanner.model.Route> otpRouteById;

    private final HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId;

    private final TripMapper tripMapper;

    private final StopTimesMapper stopTimesMapper;

    TripPatternMapper(
            OtpTransitServiceBuilder transitBuilder,
            EntityById<FeedScopedId, org.opentripplanner.model.Route> otpRouteById,
            HierarchicalMapById<Route> routeById,
            HierarchicalMapById<JourneyPattern> journeyPatternById,
            HierarchicalMap<String, String> quayIdByStopPointRef,
            HierarchicalMapById<DestinationDisplay> destinationDisplayById,
            HierarchicalMultimap<String, ServiceJourney> serviceJourneyByPatternId,
            EntityById<FeedScopedId, Stop> stopsById
    ) {
        this.routeById = routeById;
        this.serviceJourneyByPatternId = serviceJourneyByPatternId;
        this.otpRouteById = otpRouteById;
        this.transitBuilder = transitBuilder;
        this.tripMapper = new TripMapper(otpRouteById, routeById, journeyPatternById);
        this.stopTimesMapper = new StopTimesMapper(destinationDisplayById, stopsById, quayIdByStopPointRef);
    }

    void mapTripPattern(
            JourneyPattern journeyPattern
    ) {
        Collection<ServiceJourney> serviceJourneys = serviceJourneyByPatternId
                .lookup(journeyPattern.getId());

        if (serviceJourneys == null || serviceJourneys.isEmpty()) {
            LOG.warn("ServiceJourneyPattern " + journeyPattern.getId()
                    + " does not contain any serviceJourneys.");
            return;
        }

        List<Trip> trips = new ArrayList<>();
        for (ServiceJourney serviceJourney : serviceJourneys) {
            Trip trip = tripMapper.mapServiceJourney(
                    serviceJourney
            );

            List<StopTime> stopTimes = stopTimesMapper.mapToStopTimes(
                    journeyPattern,
                    trip,
                    serviceJourney.getPassingTimes().getTimetabledPassingTime()
            );

            transitBuilder.getStopTimesSortedByTrip().put(trip, stopTimes);
            trip.setTripHeadsign(getHeadsign(stopTimes));
            trips.add(trip);
        }

        // Create StopPattern from any trip (since they are part of the same JourneyPattern)
        StopPattern stopPattern = new StopPattern(
                transitBuilder.getStopTimesSortedByTrip().get(trips.get(0))
        );

        TripPattern tripPattern = new TripPattern(lookupRoute(journeyPattern), stopPattern);
        tripPattern.code = journeyPattern.getId();
        tripPattern.name = journeyPattern.getName() == null ? "" : journeyPattern.getName().getValue();

        createTripTimes(trips, tripPattern);

        transitBuilder.getTripPatterns().put(tripPattern.stopPattern, tripPattern);
    }

    private org.opentripplanner.model.Route lookupRoute(
            JourneyPattern journeyPattern
    ) {
        Route route = routeById.lookup(journeyPattern.getRouteRef().getRef());
        return otpRouteById.get(createFeedScopedId(route.getLineRef().getValue().getRef()));
    }

    private void createTripTimes(
            List<Trip> trips,
            TripPattern tripPattern
    ) {
        Deduplicator deduplicator = new Deduplicator();
        for (Trip trip : trips) {
            if (transitBuilder.getStopTimesSortedByTrip().get(trip).size() == 0) {
                LOG.warn("Trip" + trip.getId() + " does not contain any trip times.");
            } else {
                TripTimes tripTimes = new TripTimes(trip,
                        transitBuilder.getStopTimesSortedByTrip().get(trip), deduplicator);
                tripPattern.add(tripTimes);
                transitBuilder.getTripsById().add(trip);
            }
        }
    }

    private String getHeadsign(List<StopTime> stopTimes) {
        if (stopTimes != null && stopTimes.size() > 0) {
            return stopTimes.stream().findFirst().get().getStopHeadsign();
        } else {
            return "";
        }
    }
}
