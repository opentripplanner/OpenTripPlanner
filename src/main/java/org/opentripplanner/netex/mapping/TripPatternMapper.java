package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.impl.EntityById;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMapById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.FlexibleLine;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;

/**
 * Maps NeTEx JourneyPattern to OTP TripPattern. All ServiceJourneys in the same JourneyPattern contain the same
 * sequence of stops. This means that they can all use the same StopPattern. Each ServiceJourney contains
 * TimeTabledPassingTimes that are mapped to StopTimes.
 * <p>
 * Headsigns in NeTEx are only specified once and then valid for each subsequent TimeTabledPassingTime until a new
 * headsign is specified. This is accounted for in the mapper.
 * <p>
 * THIS CLASS IS NOT THREADSAFE! This mapper store its intermediate results as part of its state.
 */
class TripPatternMapper {

    private final DataImportIssueStore issueStore;

    private final FeedScopedIdFactory idFactory;

    private final EntityById<org.opentripplanner.model.Route> otpRouteById;

    private final ReadOnlyHierarchicalMap<String, Route> routeById;

    private final Multimap<String, ServiceJourney> serviceJourniesByPatternId = ArrayListMultimap.create();

    private final TripMapper tripMapper;

    private final StopTimesMapper stopTimesMapper;

    private final Deduplicator deduplicator;

    private TripPatternMapperResult result;

    TripPatternMapper(
            DataImportIssueStore issueStore,
            FeedScopedIdFactory idFactory,
            EntityById<Operator> operatorById,
            EntityById<Stop> stopsById,
            EntityById<FlexStopLocation> flexStopLocationsById,
            EntityById<FlexLocationGroup> flexLocationGroupsById,
            EntityById<org.opentripplanner.model.Route> otpRouteById,
            Set<FeedScopedId> shapePointsIds,
            ReadOnlyHierarchicalMap<String, Route> routeById,
            ReadOnlyHierarchicalMap<String, JourneyPattern> journeyPatternById,
            ReadOnlyHierarchicalMap<String, String> quayIdByStopPointRef,
            ReadOnlyHierarchicalMap<String, String> flexibleStopPlaceIdByStopPointRef,
            ReadOnlyHierarchicalMap<String, DestinationDisplay> destinationDisplayById,
            ReadOnlyHierarchicalMap<String, ServiceJourney> serviceJourneyById,
            ReadOnlyHierarchicalMapById<FlexibleLine> flexibleLinesById,
            Map<String, FeedScopedId> serviceIds,
            Deduplicator deduplicator
    ) {
        this.issueStore = issueStore;
        this.idFactory = idFactory;
        this.routeById = routeById;
        this.otpRouteById = otpRouteById;
        this.tripMapper = new TripMapper(
            idFactory,
            operatorById,
            otpRouteById,
            routeById,
            journeyPatternById,
            serviceIds,
            shapePointsIds
        );
        this.stopTimesMapper = new StopTimesMapper(
            issueStore,
            idFactory,
            stopsById,
            flexStopLocationsById,
            flexLocationGroupsById,
            destinationDisplayById,
            quayIdByStopPointRef,
            flexibleStopPlaceIdByStopPointRef,
            flexibleLinesById,
            routeById
        );
        this.deduplicator = deduplicator;

        // Index service journey by pattern id
        for (ServiceJourney sj : serviceJourneyById.localValues()) {
            this.serviceJourniesByPatternId.put(sj.getJourneyPatternRef().getValue().getRef(), sj);
        }
    }

    TripPatternMapperResult mapTripPattern(JourneyPattern journeyPattern) {
        // Make sure the result is clean, by creating a new object.
        result = new TripPatternMapperResult();
        Collection<ServiceJourney> serviceJourneys = serviceJourniesByPatternId.get(journeyPattern.getId());

        if (serviceJourneys == null || serviceJourneys.isEmpty()) {
            issueStore.add(
                    "ServiceJourneyPatternIsEmpty",
                    "ServiceJourneyPattern %s does not contain any serviceJourneys.",
                    journeyPattern.getId()
            );
            return result;
        }

        List<Trip> trips = new ArrayList<>();

        for (ServiceJourney serviceJourney : serviceJourneys) {
            Trip trip = tripMapper.mapServiceJourney(serviceJourney);

            // Unable to map ServiceJourney, problem logged by the mapper above
            if(trip == null) { continue; }

            StopTimesMapperResult stopTimes = stopTimesMapper.mapToStopTimes(
                    journeyPattern,
                    trip,
                    serviceJourney.getPassingTimes().getTimetabledPassingTime(),
                    serviceJourney
            );

            // Unable to map StopTimes, problem logged by the mapper above
            if (stopTimes == null) { continue; }

            result.scheduledStopPointsIndex.putAll(
                    serviceJourney.getId(),
                    stopTimes.scheduledStopPointIds
            );
            result.tripStopTimes.put(trip, stopTimes.stopTimes);
            result.stopTimeByNetexId.putAll(stopTimes.stopTimeByNetexId);

            trip.setTripHeadsign(getHeadsign(stopTimes.stopTimes));
            trips.add(trip);
        }

        // No trips successfully mapped
        if(trips.isEmpty()) { return result; }

        // TODO OTP2 Trips containing FlexStopLocations are not added to StopPatterns until support
        //           for this is added.
        if (
            result.tripStopTimes
                .get(trips.get(0))
                .stream()
                .anyMatch(t ->
                    t.getStop() instanceof FlexStopLocation
                    || t.getStop() instanceof FlexLocationGroup
                )
        ) {
            return result;
        }

        // Create StopPattern from any trip (since they are part of the same JourneyPattern)
        StopPattern stopPattern = deduplicator.deduplicateObject(
                StopPattern.class,
                new StopPattern(result.tripStopTimes.get(trips.get(0)))
        );

        TripPattern tripPattern = new TripPattern(
            idFactory.createId(journeyPattern.getId()),
            lookupRoute(journeyPattern),
            stopPattern
        );

        tripPattern.setName(
            journeyPattern.getName() == null ? "" : journeyPattern.getName().getValue());

        createTripTimes(trips, tripPattern);

        result.tripPatterns.add(tripPattern);

        return result;
    }

    private org.opentripplanner.model.Route lookupRoute(
            JourneyPattern journeyPattern
    ) {
        Route route = routeById.lookup(journeyPattern.getRouteRef().getRef());
        return otpRouteById.get(idFactory.createId(route.getLineRef().getValue().getRef()));
    }

    private void createTripTimes(
            List<Trip> trips,
            TripPattern tripPattern
    ) {
        for (Trip trip : trips) {
            if (result.tripStopTimes.get(trip).size() == 0) {
                issueStore.add(
                        "TripWithoutTripTimes",
                        "Trip %s does not contain any trip times.",
                        trip.getId()
                );
            } else {
                TripTimes tripTimes = new TripTimes(
                        trip,
                        result.tripStopTimes.get(trip),
                        deduplicator
                );
                tripPattern.add(tripTimes);
            }
        }
    }

    private static String getHeadsign(List<StopTime> stopTimes) {
        if (stopTimes != null && stopTimes.size() > 0) {
            return stopTimes.stream().findFirst().get().getStopHeadsign();
        } else {
            return "";
        }
    }

}
